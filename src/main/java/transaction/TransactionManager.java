package transaction;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TransactionManager {
    private final Map<SocketChannel, TransactionState> clientStates = new ConcurrentHashMap<>();

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
        clientStates.remove(client);
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
