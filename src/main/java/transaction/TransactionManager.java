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

    // Reverse index: key -> set of clients watching that key (for performance)
    private final Map<String, Set<SocketChannel>> keyToWatchingClients = new ConcurrentHashMap<>();

    private final ServerContext serverContext;

    /**
     * Constructs a TransactionManager with the given server context.
     *
     * @param serverContext the server context
     */
    public TransactionManager(final ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * Returns the TransactionState for a client, creating one if necessary.
     * For replicated commands (null client), returns a stateless TransactionState.
     *
     * @param clientChannel the client channel, or null for replicated commands
     * @return the TransactionState for the client
     */
    public TransactionState getOrCreateState(final SocketChannel clientChannel) {
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
    public boolean isInTransaction(final SocketChannel clientChannel) {
        final TransactionState transactionState = clientTransactionStates.get(clientChannel);
        return transactionState != null && transactionState.isInTransaction();
    }

    /**
     * Clears the transaction state for a client and updates metrics if needed.
     *
     * @param clientChannel the client channel
     */
    public void clearState(final SocketChannel clientChannel) {
        final TransactionState transactionState = clientTransactionStates.remove(clientChannel);
        if (transactionState != null) {
            // Clean up reverse index
            final Set<String> watchedKeys = transactionState.getWatchedKeys();
            for (final String key : watchedKeys) {
                final Set<SocketChannel> clients = keyToWatchingClients.get(key);
                if (clients != null) {
                    clients.remove(clientChannel);
                    if (clients.isEmpty()) {
                        keyToWatchingClients.remove(key);
                    }
                }
            }

            if (transactionState.isInTransaction()) {
                serverContext.getMetricsCollector().decrementActiveTransactions();
            }
            LOGGER.info("Cleared transaction state for client: {}", clientChannel);
        }
    }

    /**
     * Begins a transaction for the client and updates metrics.
     *
     * @param clientChannel the client channel
     */
    public void beginTransaction(final SocketChannel clientChannel) {
        final TransactionState transactionState = getOrCreateState(clientChannel);
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
    public void endTransaction(final SocketChannel clientChannel, final boolean success) {
        final TransactionState transactionState = clientTransactionStates.get(clientChannel);
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
    public void queueCommand(final SocketChannel clientChannel, final commands.core.Command command,
            final commands.context.CommandContext commandContext) {
        final TransactionState transactionState = getOrCreateState(clientChannel);
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
        for (final Map.Entry<SocketChannel, TransactionState> entry : clientTransactionStates.entrySet()) {
            final TransactionState state = entry.getValue();
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
        keyToWatchingClients.clear();
        LOGGER.info("Cleared all transaction states.");
    }

    /**
     * Adds a key to the set of watched keys for the client.
     *
     * @param clientChannel the client channel
     * @param key           the key to watch
     */
    public void watchKey(final SocketChannel clientChannel, final String key) {
        getOrCreateState(clientChannel).addWatchedKey(key);

        // Update reverse index for performance
        Set<SocketChannel> clients = keyToWatchingClients.get(key);
        if (clients == null) {
            clients = ConcurrentHashMap.newKeySet();
            keyToWatchingClients.put(key, clients);
        }
        clients.add(clientChannel);

        LOGGER.debug("Client {} is now watching key: {}", clientChannel, key);
    }

    /**
     * Removes all watched keys for the client.
     *
     * @param clientChannel the client channel
     */
    public void unwatchAllKeys(final SocketChannel clientChannel) {
        final TransactionState transactionState = clientTransactionStates.get(clientChannel);
        if (transactionState != null) {
            // Remove client from reverse index for all watched keys
            final Set<String> watchedKeys = transactionState.getWatchedKeys();
            for (final String key : watchedKeys) {
                final Set<SocketChannel> clients = keyToWatchingClients.get(key);
                if (clients != null) {
                    clients.remove(clientChannel);
                    if (clients.isEmpty()) {
                        keyToWatchingClients.remove(key);
                    }
                }
            }

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
    public boolean hasWatchedKeys(final SocketChannel clientChannel) {
        final TransactionState transactionState = clientTransactionStates.get(clientChannel);
        return transactionState != null && transactionState.hasWatchedKeys();
    }

    /**
     * Returns the set of watched keys for the client.
     *
     * @param clientChannel the client channel
     * @return the set of watched keys, or an empty set if none
     */
    public Set<String> getWatchedKeys(final SocketChannel clientChannel) {
        final TransactionState transactionState = clientTransactionStates.get(clientChannel);
        return transactionState != null ? transactionState.getWatchedKeys() : Set.of();
    }

    /**
     * Invalidates transactions for all clients watching the specified key.
     * Uses reverse index for O(1) key lookup instead of O(n) client iteration.
     *
     * @param key the key to invalidate
     */
    public void invalidateWatchingClients(final String key) {
        final Set<SocketChannel> watchingClients = keyToWatchingClients.get(key);
        if (watchingClients != null && !watchingClients.isEmpty()) {
            for (final SocketChannel clientChannel : watchingClients) {
                final TransactionState transactionState = clientTransactionStates.get(clientChannel);
                if (transactionState != null) {
                    transactionState.invalidateTransaction();
                    LOGGER.debug("Invalidated transaction for client {} watching key: {}", clientChannel, key);
                }
            }
        }
    }
}
