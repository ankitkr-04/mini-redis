package blocking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class BlockingManager {
    private final Map<String, Queue<BlockedClient>> waitingClients = new ConcurrentHashMap<>();
    private final StorageEngine storage;

    public BlockingManager(StorageEngine storage) {
        this.storage = storage;
    }

    public void blockClient(String key, SocketChannel client) {
        BlockedClient blockedClient = BlockedClient.indefinite(client);
        waitingClients.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>())
                .offer(blockedClient);
    }

    public void blockClient(String key, SocketChannel client, double timeoutMs) {
        BlockedClient blockedClient = BlockedClient.withTimeout(client, timeoutMs);
        waitingClients.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>())
                .offer(blockedClient);
    }

    public void notifyWaitingClients(String key, StorageEngine storage) {
        Queue<BlockedClient> queue = waitingClients.get(key);
        if (queue == null)
            return;

        // Process waiting clients while list has elements
        while (!queue.isEmpty() && storage.getListLength(key) > 0) {
            BlockedClient client = queue.poll();

            if (client == null || client.isExpired()) {
                continue;
            }

            // Try to pop a value for this client
            var value = storage.leftPop(key);
            if (value.isEmpty()) {
                break; // No more values available
            }

            // Send response to client
            try {
                ByteBuffer response = ResponseWriter.array(List.of(key, value.get()));
                writeResponse(client.channel(), response);
            } catch (IOException e) {
                // Client disconnected, ignore
                System.err.println("Failed to notify client: " + e.getMessage());
            }
        }

        // Clean up empty queue
        if (queue.isEmpty()) {
            waitingClients.remove(key);
        }
    }

    public void removeExpiredClients() {
        waitingClients.entrySet().removeIf(entry -> {
            Queue<BlockedClient> queue = entry.getValue();

            // Send timeout responses to expired clients
            queue.removeIf(client -> {
                if (client.isExpired()) {
                    try {
                        writeResponse(client.channel(), ResponseWriter.bulkString(null));
                    } catch (IOException e) {
                        // Client disconnected, ignore
                    }
                    return true;
                }
                return false;
            });

            return queue.isEmpty();
        });
    }

    private void writeResponse(SocketChannel channel, ByteBuffer response) throws IOException {
        while (response.hasRemaining()) {
            channel.write(response);
        }
    }

    public Set<String> getWaitingKeys() {
        return waitingClients.keySet();
    }

    public void clear() {
        waitingClients.clear();
    }
}
