package blocking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class ListBlockingManager extends BlockingManager<String> {

    public ListBlockingManager(StorageEngine storage) {
        super(storage);
    }

    @Override
    protected boolean hasDataAvailable(String key) {
        return storage.getListLength(key) > 0;
    }

    @Override
    protected Optional<String> retrieveData(String key) {
        return storage.leftPop(key);
    }

    @Override
    protected void sendSuccessResponse(BlockedClient client, String key, String data) {
        try {
            var response = ResponseWriter.array(List.of(key, data));
            writeResponse(client.channel(), response);
        } catch (IOException e) {
            System.err.println("Failed to send response to blocked client: " + e.getMessage());
        }
    }

    @Override
    protected void sendTimeoutResponse(BlockedClient client) {
        try {
            writeResponse(client.channel(), ResponseWriter.bulkString(null));
        } catch (IOException e) {
            System.err.println(
                    "Failed to send timeout response to blocked client: " + e.getMessage());
        }
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

            // Send timeout responses and remove expired clients
            for (BlockedClient client : expiredClients) {
                sendTimeoutResponse(client);
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
