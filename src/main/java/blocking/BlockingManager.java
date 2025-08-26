package blocking;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ProtocolConstants;
import config.ServerConfig;
import events.EventListener;
import protocol.ResponseBuilder;
import scheduler.TimeoutScheduler;
import storage.StorageService;

/**
 * Enhanced blocking manager that handles client blocking operations for various
 * data structures.
 * Provides thread-safe management of blocked clients with timeout support and
 * automatic cleanup.
 * 
 * Key improvements:
 * - Generic type support for different blocking contexts
 * - Better error handling and logging
 * - Metrics tracking
 * - Improved resource management
 * - Java 24 features utilization
 */
public final class BlockingManager implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingManager.class);

    // Core data structures with improved initial capacities
    private final Map<String, Queue<BlockedClient>> waitingClients = new ConcurrentHashMap<>(
            BlockingConstants.INITIAL_WAITING_CLIENTS_CAPACITY);

    private final Map<BlockedClient, BlockingContext<String>> clientContexts = new ConcurrentHashMap<>(
            BlockingConstants.INITIAL_CLIENT_CONTEXTS_CAPACITY);

    // Dependencies
    private final StorageService storage;
    private volatile TimeoutScheduler scheduler;

    // State management
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean cleanupScheduled = new AtomicBoolean(false);

    // Metrics for monitoring and debugging
    private final AtomicLong totalBlockedClients = new AtomicLong(0);
    private final AtomicLong totalTimeoutedClients = new AtomicLong(0);
    private final AtomicLong totalSuccessfulUnblocks = new AtomicLong(0);

    /**
     * Creates a new blocking manager with the specified storage service.
     * 
     * @param storage the storage service for data operations
     */
    public BlockingManager(StorageService storage) {
        this.storage = Objects.requireNonNull(storage, "Storage service cannot be null");
    }

    /**
     * Starts the blocking manager with the given scheduler.
     * 
     * @param scheduler the timeout scheduler for cleanup operations
     */
    public void start(TimeoutScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "Scheduler cannot be null");

        if (isRunning.compareAndSet(false, true)) {
            scheduleNextCleanup();
            LOGGER.info("BlockingManager started successfully");
        } else {
            LOGGER.warn("BlockingManager is already running");
        }
    }

    /**
     * Stops the blocking manager and cleans up all resources.
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            clear();
            LOGGER.info("BlockingManager stopped successfully");
        }
    }

    /**
     * Schedules the next cleanup operation to remove expired clients.
     */
    private void scheduleNextCleanup() {
        if (scheduler != null && isRunning.get() && cleanupScheduled.compareAndSet(false, true)) {
            scheduler.schedule(ServerConfig.CLEANUP_INTERVAL_MS, () -> {
                try {
                    removeExpiredClients();
                } catch (Exception e) {
                    LOGGER.warn("Error during cleanup operation", e);
                } finally {
                    cleanupScheduled.set(false);
                    if (isRunning.get()) {
                        scheduleNextCleanup(); // Schedule the next cleanup
                    }
                }
            });
        }
    }

    /**
     * Blocks a client for list operations (BLPOP, BRPOP, etc.).
     * 
     * @param keys      the list keys to monitor
     * @param client    the client channel to block
     * @param timeoutMs optional timeout in milliseconds
     */
    public void blockClientForLists(List<String> keys, SocketChannel client, Optional<Long> timeoutMs) {
        Objects.requireNonNull(keys, "Keys cannot be null");
        Objects.requireNonNull(client, "Client channel cannot be null");
        Objects.requireNonNull(timeoutMs, "Timeout optional cannot be null");

        validateBlockingRequest(keys);

        final ListBlockingContext context = new ListBlockingContext(keys);
        blockClient(keys, client, timeoutMs, context);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Blocked client for lists: keys={}, timeout={}",
                    keys, timeoutMs.map(String::valueOf).orElse("indefinite"));
        }
    }

    /**
     * Blocks a client for stream operations (XREAD BLOCK, etc.).
     * 
     * @param keys      the stream keys to monitor
     * @param ids       the stream IDs to start from
     * @param count     optional maximum entries per stream
     * @param client    the client channel to block
     * @param timeoutMs optional timeout in milliseconds
     */
    public void blockClientForStreams(List<String> keys, List<String> ids, Optional<Integer> count,
            SocketChannel client, Optional<Long> timeoutMs) {
        Objects.requireNonNull(keys, "Keys cannot be null");
        Objects.requireNonNull(ids, "IDs cannot be null");
        Objects.requireNonNull(count, "Count optional cannot be null");
        Objects.requireNonNull(client, "Client channel cannot be null");
        Objects.requireNonNull(timeoutMs, "Timeout optional cannot be null");

        validateBlockingRequest(keys);

        final StreamBlockingContext context = new StreamBlockingContext(keys, ids, count);
        blockClient(keys, client, timeoutMs, context);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Blocked client for streams: keys={}, ids={}, count={}, timeout={}",
                    keys, ids, count.map(String::valueOf).orElse("unlimited"),
                    timeoutMs.map(String::valueOf).orElse("indefinite"));
        }
    }

    /**
     * Validates a blocking request for common constraints.
     */
    private void validateBlockingRequest(List<String> keys) {
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("At least one key must be specified");
        }
        if (keys.size() > BlockingConstants.MAXIMUM_KEYS_PER_BLOCKING_OPERATION) {
            throw new IllegalArgumentException("Too many keys: " + keys.size());
        }
    }

    /**
     * Core method to block a client with the specified context.
     */
    private void blockClient(List<String> keys, SocketChannel client, Optional<Long> timeoutMs,
            BlockingContext<String> context) {

        if (!client.isOpen()) {
            LOGGER.warn("Attempting to block a closed client channel");
            return;
        }

        try {
            final BlockedClient blockedClient = createBlockedClient(client, timeoutMs);
            clientContexts.put(blockedClient, context);

            // Add client to all relevant key queues
            for (final String key : keys) {
                waitingClients.computeIfAbsent(key, _ -> new ConcurrentLinkedQueue<>()).offer(blockedClient);
            }

            totalBlockedClients.incrementAndGet();

        } catch (Exception e) {
            LOGGER.warn("Failed to block client", e);
            throw new RuntimeException("Failed to block client", e);
        }
    }

    @Override
    public void onDataAdded(String key) {
        Objects.requireNonNull(key, "Key cannot be null");

        final Queue<BlockedClient> queue = waitingClients.get(key);
        if (queue == null) {
            return;
        }

        final boolean queueModified = queue.removeIf(client -> processClientForDataAvailability(client, key));

        if (queueModified && queue.isEmpty()) {
            waitingClients.remove(key);
        }
    }

    @Override
    public void onDataRemoved(String key) {
        // For most blocking operations, data removal doesn't affect waiting clients
        // Subclasses can override this for specific behavior
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Data removed from key: {}", key);
        }
    }

    /**
     * Processes a client when data becomes available for a key.
     * 
     * @param client the blocked client
     * @param key    the key that has new data
     * @return true if the client should be removed from the queue, false otherwise
     */
    private boolean processClientForDataAvailability(BlockedClient client, String key) {
        // Check if client is still valid
        if (!client.isChannelOpen()) {
            cleanupClient(client);
            return true;
        }

        // Check for timeout
        if (client.isExpired()) {
            sendTimeoutResponse(client);
            cleanupClient(client);
            totalTimeoutedClients.incrementAndGet();
            return true;
        }

        // Check if data is available and send response
        final BlockingContext<String> context = clientContexts.get(client);
        if (context != null && context.hasDataAvailable(key, storage)) {
            sendSuccessResponse(client, context);
            cleanupClient(client);
            totalSuccessfulUnblocks.incrementAndGet();
            return true;
        }

        return false;
    }

    /**
     * Removes all expired clients from all queues.
     * This is called periodically by the scheduler.
     */
    public void removeExpiredClients() {
        if (!isRunning.get()) {
            return;
        }

        final AtomicLong removedCount = new AtomicLong(0);

        waitingClients.entrySet().removeIf(entry -> {
            final Queue<BlockedClient> queue = entry.getValue();
            queue.removeIf(client -> {
                if (!client.isChannelOpen() || client.isExpired()) {
                    if (client.isExpired()) {
                        sendTimeoutResponse(client);
                        totalTimeoutedClients.incrementAndGet();
                    }
                    cleanupClient(client);
                    removedCount.incrementAndGet();
                    return true;
                }
                return false;
            });
            return queue.isEmpty();
        });

        if (removedCount.get() > 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removed {} expired/disconnected clients", removedCount.get());
            }
        }
    }

    /**
     * Clears all blocked clients and sends timeout responses.
     */
    public void clear() {
        LOGGER.info("Clearing all blocked clients");

        waitingClients.values().forEach(queue -> queue.forEach(this::sendTimeoutResponse));

        waitingClients.clear();
        clientContexts.clear();

        // Reset metrics
        totalBlockedClients.set(0);
        totalTimeoutedClients.set(0);
        totalSuccessfulUnblocks.set(0);
    }

    /**
     * Creates a blocked client with appropriate timeout settings.
     */
    private BlockedClient createBlockedClient(SocketChannel client, Optional<Long> timeoutMs) {
        return timeoutMs
                .map(ms -> BlockedClient.withTimeout(client, ms))
                .orElse(BlockedClient.indefinite(client));
    }

    /**
     * Sends a success response to a client when data becomes available.
     */
    private void sendSuccessResponse(BlockedClient client, BlockingContext<String> context) {
        try {
            final ByteBuffer response = context.buildSuccessResponse(storage);
            writeResponse(client.channel(), response);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Sent success response to client: {}", context.getOperationType());
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to send success response to client", e);
        }
    }

    /**
     * Sends a timeout response to a client.
     */
    private void sendTimeoutResponse(BlockedClient client) {
        try {
            final ByteBuffer response = ResponseBuilder.encode(ProtocolConstants.RESP_NULL_ARRAY);
            writeResponse(client.channel(), response);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Sent timeout response to client");
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to send timeout response to client", e);
        }
    }

    /**
     * Writes a response to a client channel with proper error handling.
     */
    private void writeResponse(SocketChannel channel, ByteBuffer response) {
        try {
            if (channel.isOpen()) {
                while (response.hasRemaining()) {
                    channel.write(response);
                }
            }
        } catch (Exception e) {
            // Log error but don't throw to ensure cleanup continues
            LOGGER.warn(BlockingConstants.RESPONSE_WRITE_ERROR_PREFIX + e.getMessage(), e);
        }
    }

    /**
     * Cleans up a client from all data structures.
     */
    private void cleanupClient(BlockedClient client) {
        try {
            clientContexts.remove(client);
            waitingClients.values().forEach(queue -> queue.remove(client));

        } catch (Exception e) {
            LOGGER.warn(BlockingConstants.CLIENT_CLEANUP_ERROR_PREFIX + e.getMessage(), e);
        }
    }

    // Metrics and monitoring methods

    /**
     * Gets the total number of currently blocked clients.
     */
    public long getTotalBlockedClients() {
        return totalBlockedClients.get();
    }

    /**
     * Gets the total number of clients that have timed out.
     */
    public long getTotalTimeoutedClients() {
        return totalTimeoutedClients.get();
    }

    /**
     * Gets the total number of successful unblocks.
     */
    public long getTotalSuccessfulUnblocks() {
        return totalSuccessfulUnblocks.get();
    }

    /**
     * Gets the current number of blocked clients.
     */
    public int getCurrentBlockedClientCount() {
        return clientContexts.size();
    }

    /**
     * Gets the number of keys currently being monitored.
     */
    public int getMonitoredKeyCount() {
        return waitingClients.size();
    }

    /**
     * Checks if the blocking manager is currently running.
     */
    public boolean isRunning() {
        return isRunning.get();
    }
}