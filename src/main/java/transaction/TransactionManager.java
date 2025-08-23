package transaction;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TransactionManager {
    private final Map<SocketChannel, TransactionState> clientStates = new ConcurrentHashMap<>();

    public TransactionState getOrCreateState(SocketChannel client) {
        return clientStates.computeIfAbsent(client, _ -> new TransactionState());
    }

    public void clearState(SocketChannel client) {
        clientStates.remove(client);
    }

    public void clearAll() {
        clientStates.clear();
    }
}
