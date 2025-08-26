package transaction;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import server.ServerContext;

public final class TransactionManager {
    private final Map<SocketChannel, TransactionState> clientStates = new ConcurrentHashMap<>();
    private final ServerContext serverContext;

    public TransactionManager(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    public TransactionState getOrCreateState(SocketChannel client) {
        if (client == null) {
            // For replicated commands, return a new transaction state that's not in a
            // transaction
            return new TransactionState();
        }
        return clientStates.computeIfAbsent(client, _ -> new TransactionState());
    }

    public boolean isInTransaction(SocketChannel client) {
        TransactionState state = clientStates.get(client);
        return state != null && state.isInTransaction();
    }

    public void clearState(SocketChannel client) {
        TransactionState state = clientStates.remove(client);
        if (state != null && state.isInTransaction()) {
            serverContext.getMetricsCollector().decrementActiveTransactions();
        }
    }

    public void beginTransaction(SocketChannel client) {
        TransactionState state = getOrCreateState(client);
        if (!state.isInTransaction()) {
            state.beginTransaction();
            serverContext.getMetricsCollector().incrementActiveTransactions();
        }
    }

    public void endTransaction(SocketChannel client, boolean success) {
        TransactionState state = clientStates.get(client);
        if (state != null && state.isInTransaction()) {
            state.clearTransaction();
            serverContext.getMetricsCollector().decrementActiveTransactions();
            if (!success) {
                serverContext.getMetricsCollector().incrementFailedTransactions();
            }
        }
    }

    public void queueCommand(SocketChannel client, commands.core.Command command,
            commands.context.CommandContext context) {
        TransactionState state = getOrCreateState(client);
        if (state.isInTransaction()) {
            state.queueCommand(command, context);
            serverContext.getMetricsCollector().incrementTransactionCommands();
        }
    }

    public void clearAll() {
        clientStates.clear();
    }

    public void watchKey(SocketChannel client, String key) {
        getOrCreateState(client).addWatchedKey(key);
    }

    public void unwatchAllKeys(SocketChannel client) {
        TransactionState state = clientStates.get(client);
        if (state != null) {
            state.clearWatchedKeys();
        }
    }

    public boolean hasWatchedKeys(SocketChannel client) {
        TransactionState state = clientStates.get(client);
        return state != null && state.hasWatchedKeys();
    }

    public Set<String> getWatchedKeys(SocketChannel client) {
        TransactionState state = clientStates.get(client);
        return state != null ? state.getWatchedKeys() : Set.of();
    }

    public void invalidateWatchingClients(String key) {
        for (TransactionState state : clientStates.values()) {
            if (state.isKeyWatched(key)) {
                state.invalidateTransaction();
            }
        }
    }
}
