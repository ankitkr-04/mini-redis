package blocking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;
import storage.types.streams.StreamRangeEntry;

/**
 * Blocking manager for Redis stream operations (XREAD).
 * Handles multiple keys and complex stream entry responses.
 */
public final class StreamBlockingManager extends BlockingManager<List<StreamRangeEntry>> {

    // Map to store the "greater than" ID for each blocked client and key
    private final Map<BlockedClient, Map<String, String>> clientStreamIds =
            new ConcurrentHashMap<>();

    public StreamBlockingManager(StorageEngine storage) {
        super(storage);
    }

    /**
     * Block client for stream reading with multiple keys and their corresponding IDs.
     */
    public void blockClientForStreams(List<String> keys, List<String> ids,
            SocketChannel client, Optional<Long> timeoutMs) {
        BlockedClient blockedClient = timeoutMs
                .map(ms -> BlockedClient.withTimeout(client, ms.doubleValue()))
                .orElse(BlockedClient.indefinite(client));

        // Store the stream IDs for this client
        var streamIds = new ConcurrentHashMap<String, String>();
        for (int i = 0; i < keys.size() && i < ids.size(); i++) {
            streamIds.put(keys.get(i), ids.get(i));
        }
        clientStreamIds.put(blockedClient, streamIds);

        // Add client to waiting queues for all keys
        for (String key : keys) {
            waitingClients.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>())
                    .offer(blockedClient);
        }
    }

    @Override
    protected boolean hasDataAvailable(String key) {
        // For streams, we need to check if there are entries after the stored ID
        // This is a simplified check - in practice, you'd need to check against stored IDs
        return storage.getLastStreamId(key).isPresent();
    }

    @Override
    protected Optional<List<StreamRangeEntry>> retrieveData(String key) {
        // This is simplified - you'd need to get entries after the stored ID for the client
        // For now, returning empty to show the pattern
        return Optional.empty();
    }

    /**
     * Enhanced notification for streams - checks all keys a client is waiting for.
     */
    @Override
    public void notifyWaitingClients(String key) {
        var queue = waitingClients.get(key);
        if (queue == null)
            return;

        // Process all waiting clients for this key
        List<BlockedClient> toRemove = new ArrayList<>();

        for (BlockedClient client : queue) {
            if (client.isExpired()) {
                sendTimeoutResponse(client);
                cleanupClient(client);
                toRemove.add(client);
                continue;
            }

            // Check if this client has new data available across all their watched streams
            var streamIds = clientStreamIds.get(client);
            if (streamIds != null && hasNewDataForClient(client, streamIds)) {
                var allData = collectDataForClient(client, streamIds);
                if (!allData.isEmpty()) {
                    sendStreamSuccessResponse(client, allData);
                    cleanupClient(client);
                    toRemove.add(client);
                }
            }
        }

        // Remove processed clients from the queue
        toRemove.forEach(queue::remove);

        if (queue.isEmpty()) {
            waitingClients.remove(key);
        }
    }

    private boolean hasNewDataForClient(BlockedClient client, Map<String, String> streamIds) {
        return streamIds.entrySet().stream()
                .anyMatch(entry -> {
                    String key = entry.getKey();
                    String afterId = entry.getValue();
                    try {
                        return storage.getStreamAfter(key, afterId, 1).size() > 0;
                    } catch (Exception e) {
                        // If there's an error checking the stream, assume no data
                        return false;
                    }
                });
    }

    private Map<String, List<StreamRangeEntry>> collectDataForClient(
            BlockedClient client, Map<String, String> streamIds) {
        var result = new ConcurrentHashMap<String, List<StreamRangeEntry>>();

        for (var entry : streamIds.entrySet()) {
            String key = entry.getKey();
            String afterId = entry.getValue();
            try {
                var entries = storage.getStreamAfter(key, afterId, -1); // Get all new entries
                if (!entries.isEmpty()) {
                    result.put(key, entries);
                }
            } catch (Exception e) {
                // Skip this stream if there's an error
                System.err
                        .println("Error collecting data for stream " + key + ": " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Builds XREAD response format: array of [stream_name, [entries...]]
     */
    private ByteBuffer buildXReadResponse(Map<String, List<StreamRangeEntry>> streamData) {
        if (streamData.isEmpty()) {
            return ResponseWriter.arrayOfBuffers(); // *0
        }

        List<ByteBuffer> streamResponses = new ArrayList<>();

        for (var entry : streamData.entrySet()) {
            String streamKey = entry.getKey();
            List<StreamRangeEntry> entries = entry.getValue();

            if (!entries.isEmpty()) {
                streamResponses.add(
                        ResponseWriter.arrayOfBuffers(
                                ResponseWriter.bulkString(streamKey),
                                ResponseWriter.streamEntries(entries,
                                        e -> e.id(),
                                        e -> e.fieldList())));
            }
        }

        return ResponseWriter.arrayOfBuffers(streamResponses);
    }

    private void sendStreamSuccessResponse(BlockedClient client,
            Map<String, List<StreamRangeEntry>> streamData) {
        try {
            var response = buildXReadResponse(streamData);
            writeResponse(client.channel(), response);
        } catch (IOException e) {
            System.err.println("Failed to notify stream client: " + e.getMessage());
        }
    }

    @Override
    protected void sendSuccessResponse(BlockedClient client, String key,
            List<StreamRangeEntry> data) {
        // This method is called by the template method, but for streams we use the enhanced version
        // above
        var streamData = Map.of(key, data);
        sendStreamSuccessResponse(client, streamData);
    }

    @Override
    protected void sendTimeoutResponse(BlockedClient client) {
        try {
            writeResponse(client.channel(), ResponseWriter.bulkString(null));
        } catch (IOException e) {
            System.err
                    .println("Failed to send timeout response to stream client: " + e.getMessage());
        }
    }

    private void cleanupClient(BlockedClient client) {
        clientStreamIds.remove(client);
        // Remove client from all waiting queues
        waitingClients.values().forEach(queue -> queue.remove(client));
    }

    @Override
    public void clear() {
        super.clear();
        clientStreamIds.clear();
    }

    /**
     * Remove expired clients and send timeout responses
     */
    @Override
    public void removeExpiredClients() {
        // Process all waiting queues
        List<String> keysToRemove = new ArrayList<>();

        for (var entry : waitingClients.entrySet()) {
            String key = entry.getKey();
            var queue = entry.getValue();

            // Remove expired clients and send timeout responses
            List<BlockedClient> expiredClients = new ArrayList<>();
            for (BlockedClient client : queue) {
                if (client.isExpired()) {
                    expiredClients.add(client);
                }
            }

            // Send timeout responses and cleanup
            for (BlockedClient client : expiredClients) {
                sendTimeoutResponse(client);
                cleanupClient(client);
                queue.remove(client);
            }

            // Mark empty queues for removal
            if (queue.isEmpty()) {
                keysToRemove.add(key);
            }
        }

        // Remove empty queues
        for (String key : keysToRemove) {
            waitingClients.remove(key);
        }
    }
}
