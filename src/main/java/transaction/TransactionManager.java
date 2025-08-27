package transaction;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.ServerContext;

/**
 * TransactionManager manages transaction states for clients, supporting
 * MULTI/EXEC and WATCH functionality.
 * It handles queuing commands, tracking watched keys, and transaction metrics.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class TransactionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);

    // Stateless TransactionState for replicated commands (no client)
    private static final TransactionState REPLICATED_COMMAND_STATE = new TransactionState();

    // Map of client channels to their transaction states
    private final Map<SocketChannel, TransactionState> clientTransactionStates = new ConcurrentHashMap<>();

    private final ServerContext serverContext;

    /**
     * Constructs a TransactionManager with the given server context.
     *
     * @param serverContext the server context
     */
    public TransactionManager(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * Returns the TransactionState for a client, creating one if necessary.
     * For replicated commands (null client), returns a stateless TransactionState.
     *
     * @param clientChannel the client channel, or null for replicated commands
     * @return the TransactionState for the client
     */
    public TransactionState getOrCreateState(SocketChannel clientChannel) {
        if (clientChannel == null) {
            return REPLICATED_COMMAND_STATE;
        }
        return clientTransactionStates.computeIfAbsent(clientChannel, ch -> new TransactionState());
    }

    /**
     * Checks if the client is currently in a transaction.
     *
     * @param clientChannel the client channel
     * @return true if the client is in a transaction, false otherwise
     */
    public boolean isInTransaction(SocketChannel clientChannel) {
        TransactionState transactionState = clientTransactionStates.get(clientChannel);
        return transactionState != null && transactionState.isInTransaction();
    }

    /**
     * Clears the transaction state for a client and updates metrics if needed.
     *
     * @param clientChannel the client channel
     */
    public void clearState(SocketChannel clientChannel) {
        TransactionState transactionState = clientTransactionStates.remove(clientChannel);
        if (transactionState != null && transactionState.isInTransaction()) {
            serverContext.getMetricsCollector().decrementActiveTransactions();
            LOGGER.info("Cleared transaction state for client: {}", clientChannel);
        }
    }

    /**
     * Begins a transaction for the client and updates metrics.
     *
     * @param clientChannel the client channel
     */
    public void beginTransaction(SocketChannel clientChannel) {
        TransactionState transactionState = getOrCreateState(clientChannel);
        if (!transactionState.isInTransaction()) {
            transactionState.beginTransaction();
            serverContext.getMetricsCollector().incrementActiveTransactions();
            LOGGER.info("Transaction started for client: {}", clientChannel);
        }
    }

    /**
     * Ends a transaction for the client, updates metrics, and logs failures.
     *
     * @param clientChannel the client channel
     * @param success       true if the transaction succeeded, false otherwise
     */
    public void endTransaction(SocketChannel clientChannel, boolean success) {
        TransactionState transactionState = clientTransactionStates.get(clientChannel);
        if (transactionState != null && transactionState.isInTransaction()) {
            transactionState.clearTransaction();
            serverContext.getMetricsCollector().decrementActiveTransactions();
            if (!success) {
                serverContext.getMetricsCollector().incrementFailedTransactions();
                LOGGER.warn("Transaction failed for client: {}", clientChannel);
            } else {
                LOGGER.info("Transaction ended successfully for client: {}", clientChannel);
            }
        }
    }

    /**
     * Queues a command for execution in the client's transaction.
     *
     * @param clientChannel  the client channel
     * @param command        the command to queue
     * @param commandContext the command context
     */
    public void queueCommand(SocketChannel clientChannel, commands.core.Command command,
            commands.context.CommandContext commandContext) {
        TransactionState transactionState = getOrCreateState(clientChannel);
        if (transactionState.isInTransaction()) {
            transactionState.queueCommand(command, commandContext);
            serverContext.getMetricsCollector().incrementTransactionCommands();
            LOGGER.debug("Queued command in transaction for client: {}", clientChannel);
        }
    }

    /**
     * Invalidates all transactions for all clients,
     * regardless of which keys they are watching.
     * Useful when the entire store is cleared.
     */
    public void invalidateAllWatchingClients() {
        for (Map.Entry<SocketChannel, TransactionState> entry : clientTransactionStates.entrySet()) {
            TransactionState state = entry.getValue();
            if (state.hasWatchedKeys()) {
                state.invalidateTransaction();
                LOGGER.debug("Invalidated transaction for client {} due to store clear", entry.getKey());
            }
        }
        LOGGER.info("Invalidated all watching clients due to store clear.");
    }

    /**
     * Clears all transaction states for all clients.
     */
    public void clearAll() {
        clientTransactionStates.clear();
        LOGGER.info("Cleared all transaction states.");
    }

    /**
     * Adds a key to the set of watched keys for the client.
     *
     * @param clientChannel the client channel
     * @param key           the key to watch
     */
    public void watchKey(SocketChannel clientChannel, String key) {
        getOrCreateState(clientChannel).addWatchedKey(key);
        LOGGER.debug("Client {} is now watching key: {}", clientChannel, key);
    }

    /**
     * Removes all watched keys for the client.
     *
     * @param clientChannel the client channel
     */
    public void unwatchAllKeys(SocketChannel clientChannel) {
        TransactionState transactionState = clientTransactionStates.get(clientChannel);
        if (transactionState != null) {
            transactionState.clearWatchedKeys();
            LOGGER.debug("Client {} unwatched all keys.", clientChannel);
        }
    }

    /**
     * Checks if the client has any watched keys.
     *
     * @param clientChannel the client channel
     * @return true if the client has watched keys, false otherwise
     */
    public boolean hasWatchedKeys(SocketChannel clientChannel) {
        TransactionState transactionState = clientTransactionStates.get(clientChannel);
        return transactionState != null && transactionState.hasWatchedKeys();
    }

    /**
     * Returns the set of watched keys for the client.
     *
     * @param clientChannel the client channel
     * @return the set of watched keys, or an empty set if none
     */
    public Set<String> getWatchedKeys(SocketChannel clientChannel) {
        TransactionState transactionState = clientTransactionStates.get(clientChannel);
        return transactionState != null ? transactionState.getWatchedKeys() : Set.of();
    }

    /**
     * Invalidates transactions for all clients watching the specified key.
     *
     * @param key the key to invalidate
     */
    public void invalidateWatchingClients(String key) {
        for (TransactionState transactionState : clientTransactionStates.values()) {
            if (transactionState.isKeyWatched(key)) {
                transactionState.invalidateTransaction();
                LOGGER.debug("Invalidated transaction for client watching key: {}", key);
            }
        }
    }
}
