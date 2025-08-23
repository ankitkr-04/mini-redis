package blocking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import storage.interfaces.StorageEngine;

public abstract sealed class BlockingManager<T> permits ListBlockingManager, StreamBlockingManager {
    protected final Map<String, Queue<BlockedClient>> waitingClients = new ConcurrentHashMap<>();
    protected final StorageEngine storage;

    protected BlockingManager(StorageEngine storage) {
        this.storage = storage;
    }

    // Public API
    public final void blockClient(String key, SocketChannel client, Optional<Double> timeoutMs) {
        BlockedClient blockedClient = timeoutMs
                .map(t -> BlockedClient.withTimeout(client, t))
                .orElseGet(() -> BlockedClient.indefinite(client));

        waitingClients.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>())
                .offer(blockedClient);
    }

    public final void blockClient(String key, SocketChannel client) {
        blockClient(key, client, Optional.empty());
    }

    public void notifyWaitingClients(String key) {
        Queue<BlockedClient> queue = waitingClients.get(key);
        if (queue == null)
            return;

        while (!queue.isEmpty() && hasDataAvailable(key)) {
            BlockedClient client = queue.poll();

            if (client == null || client.isExpired()) {
                continue;
            }

            var data = retrieveData(key);
            if (data.isPresent()) {
                sendSuccessResponse(client, key, data.get());
            } else {
                break;
            }
        }

        if (queue.isEmpty()) {
            waitingClients.remove(key);
        }
    }

    public void removeExpiredClients() {
        for (var entry : waitingClients.entrySet()) {
            Queue<BlockedClient> queue = entry.getValue();
            queue.removeIf(BlockedClient::isExpired);
            if (queue.isEmpty()) {
                waitingClients.remove(entry.getKey());
            }
        }
    }

    public final Set<String> getWaitingKeys() {
        return waitingClients.keySet();
    }

    // Utility Methods
    protected final void writeResponse(SocketChannel channel, ByteBuffer response)
            throws IOException {
        while (response.hasRemaining()) {
            channel.write(response);
        }
    }

    public void clear() {
        waitingClients.clear();
    }

    // Protected API
    protected abstract boolean hasDataAvailable(String key);

    protected abstract Optional<T> retrieveData(String key);

    protected abstract void sendSuccessResponse(BlockedClient client, String key, T data);

    protected abstract void sendTimeoutResponse(BlockedClient client);


}
