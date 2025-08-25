package transaction;

import java.nio.channels.SocketChannel;
import java.util.Map;
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
}
