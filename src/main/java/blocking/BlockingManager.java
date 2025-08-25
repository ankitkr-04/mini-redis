package blocking;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import config.ServerConfig;
import events.EventListener;
import protocol.ResponseBuilder;
import scheduler.TimeoutScheduler;
import storage.StorageService;

public final class BlockingManager implements EventListener {
    private final Map<String, Queue<BlockedClient>> waitingClients = new ConcurrentHashMap<>();
    private final Map<BlockedClient, BlockingContext> clientContexts = new ConcurrentHashMap<>();
    private final StorageService storage;

    public BlockingManager(StorageService storage) {
        this.storage = storage;
    }

    public void start(TimeoutScheduler scheduler) {
        scheduler.schedule(ServerConfig.CLEANUP_INTERVAL_MS, this::removeExpiredClients);
    }

    public void blockClientForLists(List<String> keys, SocketChannel client,
            Optional<Long> timeoutMs) {
        blockClient(keys, client, timeoutMs, new ListBlockingContext(keys));
    }

    public void blockClientForStreams(List<String> keys, List<String> ids, Optional<Integer> count,
            SocketChannel client, Optional<Long> timeoutMs) {
        blockClient(keys, client, timeoutMs, new StreamBlockingContext(keys, ids, count));
    }

    private void blockClient(List<String> keys, SocketChannel client, Optional<Long> timeoutMs,
            BlockingContext context) {
        BlockedClient blockedClient = createBlockedClient(client, timeoutMs);
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

        queue.removeIf(client -> processClient(client, key));

        if (queue.isEmpty()) {
            waitingClients.remove(key);
        }
    }

    private boolean processClient(BlockedClient client, String key) {
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
    }

    @Override
    public void onDataRemoved(String key) {
    }

    public void removeExpiredClients() {
        waitingClients.entrySet().removeIf(entry -> {
            Queue<BlockedClient> queue = entry.getValue();
            queue.removeIf(client -> {
                if (client.isExpired()) {
                    sendTimeoutResponse(client);
                    cleanupClient(client);
                    return true;
                }
                return false;
            });
            return queue.isEmpty();
        });
    }

    public void clear() {
        waitingClients.clear();
        clientContexts.clear();
    }

    private BlockedClient createBlockedClient(SocketChannel client, Optional<Long> timeoutMs) {
        return timeoutMs.map(ms -> BlockedClient.withTimeout(client, ms))
                .orElse(BlockedClient.indefinite(client));
    }

    private void sendSuccessResponse(BlockedClient client, BlockingContext context) {
        writeResponse(client.channel(), context.buildSuccessResponse(storage));
    }

    private void sendTimeoutResponse(BlockedClient client) {
        writeResponse(client.channel(), ResponseBuilder.bulkString(null));
    }

    private void writeResponse(SocketChannel channel, ByteBuffer response) {
        try {
            while (response.hasRemaining())
                channel.write(response);
        } catch (Exception ignored) {
        }
    }

    private void cleanupClient(BlockedClient client) {
        clientContexts.remove(client);
        waitingClients.values().forEach(q -> q.remove(client));
    }
}
