package blocking;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import events.StorageEventListener;
import protocol.ResponseBuilder;
import storage.StorageService;

public final class BlockingManager implements StorageEventListener {
    private final Map<String, Queue<BlockedClient>> waitingClients = new ConcurrentHashMap<>();
    private final Map<BlockedClient, BlockingContext> clientContexts = new ConcurrentHashMap<>();
    private final StorageService storage;

    public BlockingManager(StorageService storage) {
        this.storage = storage;
    }

    // Block client for list operations
    public void blockClientForLists(List<String> keys, SocketChannel client,
            Optional<Long> timeoutMs) {
        BlockedClient blockedClient = createBlockedClient(client, timeoutMs);
        BlockingContext context = new ListBlockingContext(keys);

        clientContexts.put(blockedClient, context);
        for (String key : keys) {
            waitingClients.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>())
                    .offer(blockedClient);
        }
    }

    // Block client for stream operations
    public void blockClientForStreams(List<String> keys, List<String> ids, Optional<Integer> count,
            SocketChannel client, Optional<Long> timeoutMs) {
        BlockedClient blockedClient = createBlockedClient(client, timeoutMs);

        StreamBlockingContext context = new StreamBlockingContext(keys, ids, count);
        clientContexts.put(blockedClient, context);

        for (String key : keys) {
            waitingClients.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>())
                    .offer(blockedClient);
        }
    }

    @Override
    public void onDataAdded(String key) {
        Queue<BlockedClient> queue = waitingClients.get(key);
        if (queue == null)
            return;

        queue.removeIf(client -> {
            if (client.isExpired()) {
                sendTimeoutResponse(client);
                cleanupClient(client);
                return true;
            }

            BlockingContext context = clientContexts.get(client);
            if (context != null && context.hasDataAvailable(key, storage)) {
                sendSuccessResponse(client, context);
                cleanupClient(client);
                return true;
            }

            return false;
        });

        if (queue.isEmpty()) {
            waitingClients.remove(key);
        }
    }

    @Override
    public void onDataRemoved(String key) {
        // Not typically needed for Redis operations
    }

    public void removeExpiredClients() {
        for (var entry : waitingClients.entrySet()) {
            Queue<BlockedClient> queue = entry.getValue();

            queue.removeIf(client -> {
                if (client.isExpired()) {
                    sendTimeoutResponse(client);
                    cleanupClient(client);
                    return true;
                }
                return false;
            });

            if (queue.isEmpty()) {
                waitingClients.remove(entry.getKey());
            }
        }
    }

    public void clear() {
        waitingClients.clear();
        clientContexts.clear();
    }

    private BlockedClient createBlockedClient(SocketChannel client, Optional<Long> timeoutMs) {
        return timeoutMs
                .map(ms -> BlockedClient.withTimeout(client, ms))
                .orElse(BlockedClient.indefinite(client));
    }

    private void sendSuccessResponse(BlockedClient client, BlockingContext context) {
        try {
            ByteBuffer response = context.buildSuccessResponse(storage);
            writeResponse(client.channel(), response);
        } catch (Exception e) {
            // Silently handle errors
        }
    }

    private void sendTimeoutResponse(BlockedClient client) {
        try {
            writeResponse(client.channel(), ResponseBuilder.bulkString(null));
        } catch (Exception e) {
            // Silently handle errors
        }
    }

    private void writeResponse(SocketChannel channel, ByteBuffer response) throws Exception {
        while (response.hasRemaining()) {
            channel.write(response);
        }
    }

    private void cleanupClient(BlockedClient client) {
        clientContexts.remove(client);
        waitingClients.values().forEach(queue -> queue.remove(client));
    }
}
