package store;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import record.BlockedClient;

public class BlockingClientStore {

    private final Map<String, Queue<BlockedClient>> store = new ConcurrentHashMap<>();

    /**
     * Registers a client as waiting for a specific key (list).
     * 
     * @param key the list key
     * @param client the client's socket channel
     * @param timeoutMs 0 for indefinite, or milliseconds to wait
     */
    public void addClient(String key, SocketChannel client, long timeoutMs) {
        long timeoutEnd = timeoutMs == 0 ? 0 : System.currentTimeMillis() + timeoutMs;
        BlockedClient blockedClient = new BlockedClient(client, timeoutEnd);
        store.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(blockedClient);
    }

    public void addClient(String key, SocketChannel client) {
        var blockedClient = BlockedClient.withOutExpiry(client);

        store.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(blockedClient);
    }



    /**
     * Removes a specific client from waiting for a key.
     */
    public void removeClient(String key, SocketChannel client) {
        Queue<BlockedClient> queue = store.get(key);
        if (queue != null) {
            queue.removeIf(bc -> bc.client().equals(client));
            if (queue.isEmpty()) {
                store.remove(key);
            }
        }
    }

    /**
     * Pops the first waiting client for a key (FIFO). Returns null if no clients are waiting.
     */
    public BlockedClient pollClient(String key) {
        Queue<BlockedClient> queue = store.get(key);
        if (queue == null)
            return null;

        BlockedClient blocked = queue.poll();
        if (queue.isEmpty()) {
            store.remove(key);
        }
        return blocked;
    }

    /**
     * Checks if there are any clients waiting for a key.
     */
    public boolean hasClients(String key) {
        Queue<BlockedClient> queue = store.get(key);
        return queue != null && !queue.isEmpty();
    }

    /**
     * Removes and returns all expired clients for a key.
     */
    public Queue<BlockedClient> removeExpiredClients(String key) {
        Queue<BlockedClient> expired = new ConcurrentLinkedQueue<>();
        Queue<BlockedClient> queue = store.get(key);
        if (queue != null) {
            queue.removeIf(bc -> {
                boolean expiredFlag = bc.isExpired();
                if (expiredFlag)
                    expired.add(bc);
                return expiredFlag;
            });
            if (queue.isEmpty())
                store.remove(key);
        }
        return expired;
    }

    /**
     * Clears all blocked clients.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Returns all keys with blocked clients.
     */
    public Map<String, Queue<BlockedClient>> getAll() {
        return store;
    }
}
