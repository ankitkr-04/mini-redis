package metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Central metrics collector for Redis server implementation.
 * 
 * <p>
 * Collects and tracks various metrics including server-wide, command-specific,
 * storage, replication, pub/sub, transaction, and persistence metrics.
 * Enhanced to be compatible with Redis Enterprise Software metrics.
 * </p>
 * 
 * <p>
 * This collector uses Micrometer for metrics collection and provides
 * Redis-compatible metric names and formats for monitoring integration.
 * </p>
 * 
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public final class MetricsCollector {

    /** Logger instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class);

    /** Initial capacity for command-specific metric maps */
    private static final int INITIAL_COMMAND_MAP_CAPACITY = 64;

    private final MeterRegistry meterRegistry;
    private final Instant startTime;

    // Client Connection Metrics (Redis Enterprise compatible)
    private final Counter clientConnections;
    private final Counter clientDisconnections;
    private final Counter clientConnectionFailures;
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    // Command Metrics with Redis Enterprise categories
    private final Counter readRequests;
    private final Counter writeRequests;
    private final Counter otherRequests;
    private final Counter readResponses;
    private final Counter writeResponses;
    private final Counter otherResponses;
    private final Timer readRequestsLatency;
    private final Timer writeRequestsLatency;
    private final Timer otherRequestsLatency;

    // Legacy metrics
    private final Counter totalCommandsProcessed;
    private final Counter totalErrors;
    private final AtomicLong memoryUsage = new AtomicLong(0);

    // Command metrics (detailed)
    private final Map<String, Counter> commandCounters = new ConcurrentHashMap<>(INITIAL_COMMAND_MAP_CAPACITY);
    private final Map<String, Timer> commandTimers = new ConcurrentHashMap<>(INITIAL_COMMAND_MAP_CAPACITY);
    private final Map<String, Counter> commandErrors = new ConcurrentHashMap<>(INITIAL_COMMAND_MAP_CAPACITY);

    // Key Type Storage metrics (Redis Enterprise style)
    private final AtomicInteger totalKeys = new AtomicInteger(0);
    private final AtomicInteger stringKeys = new AtomicInteger(0);
    private final AtomicInteger listKeys = new AtomicInteger(0);
    private final AtomicInteger setKeys = new AtomicInteger(0);
    private final AtomicInteger zsetKeys = new AtomicInteger(0);
    private final AtomicInteger hashKeys = new AtomicInteger(0);
    private final AtomicInteger streamKeys = new AtomicInteger(0);

    // Key Size Distribution (Redis Enterprise style)
    private final AtomicInteger stringsUnder128M = new AtomicInteger(0);
    private final AtomicInteger strings128MTo512M = new AtomicInteger(0);
    private final AtomicInteger stringsOver512M = new AtomicInteger(0);

    private final AtomicInteger listsUnder1M = new AtomicInteger(0);
    private final AtomicInteger lists1MTo8M = new AtomicInteger(0);
    private final AtomicInteger listsOver8M = new AtomicInteger(0);

    private final AtomicInteger setsUnder1M = new AtomicInteger(0);
    private final AtomicInteger sets1MTo8M = new AtomicInteger(0);
    private final AtomicInteger setsOver8M = new AtomicInteger(0);

    private final AtomicInteger zsetsUnder1M = new AtomicInteger(0);
    private final AtomicInteger zsets1MTo8M = new AtomicInteger(0);
    private final AtomicInteger zsetsOver8M = new AtomicInteger(0);

    // Storage metrics
    private final Map<String, AtomicInteger> keyCountByType = new ConcurrentHashMap<>();
    private final Counter expiredKeys;
    private final Counter evictedKeys;
    private final Counter keysRead;
    private final Counter keysWritten;
    private final Counter keyspaceReadHits;
    private final Counter keyspaceReadMisses;
    private final Counter keyspaceWriteHits;
    private final Counter keyspaceWriteMisses;

    // Replication metrics (Redis Enterprise compatible)
    private final AtomicLong replicationLag = new AtomicLong(0);
    private final AtomicInteger connectedReplicas = new AtomicInteger(0);
    private final Counter replicationCommandsSent;
    private final AtomicLong masterReplOffset = new AtomicLong(0);
    private final AtomicInteger masterSyncInProgress = new AtomicInteger(0);

    // Pub/Sub metrics (Redis Enterprise compatible)
    private final AtomicInteger activeChannels = new AtomicInteger(0);
    private final Counter messagesPublished;
    private final Map<String, AtomicInteger> subscribersPerChannel = new ConcurrentHashMap<>();

    // Transaction metrics (Redis Enterprise compatible)
    private final AtomicInteger activeTransactions = new AtomicInteger(0);
    private final Counter transactionCommands;
    private final Counter failedTransactions;

    // Persistence metrics (Redis Enterprise compatible)
    private final Counter rdbSaves;
    private final Counter aofWrites;
    private final Counter persistenceErrors;
    private final AtomicInteger rdbBgsaveInProgress = new AtomicInteger(0);

    // Blocking operations metrics
    private final AtomicInteger blockedClients = new AtomicInteger(0);

    // Network metrics
    private final Counter totalNetInputBytes;
    private final Counter totalNetOutputBytes;
    private final Counter totalConnectionsReceived;

    public MetricsCollector() {
        this.startTime = Instant.now();
        this.meterRegistry = new SimpleMeterRegistry();

        // Initialize client connection metrics (Redis Enterprise style)
        this.clientConnections = Counter.builder("endpoint_client_connections")
                .description("Number of client connection establishment events")
                .register(meterRegistry);

        this.clientDisconnections = Counter.builder("endpoint_client_disconnections")
                .description("Number of client disconnections initiated by the client")
                .register(meterRegistry);

        this.clientConnectionFailures = Counter.builder("endpoint_client_establishment_failures")
                .description("Number of client connections that failed to establish properly")
                .register(meterRegistry);

        // Initialize command metrics by type (Redis Enterprise style)
        this.readRequests = Counter.builder("endpoint_read_requests")
                .description("Number of read requests")
                .register(meterRegistry);

        this.writeRequests = Counter.builder("endpoint_write_requests")
                .description("Number of write requests")
                .register(meterRegistry);

        this.otherRequests = Counter.builder("endpoint_other_requests")
                .description("Number of other requests")
                .register(meterRegistry);

        this.readResponses = Counter.builder("endpoint_read_responses")
                .description("Number of read responses")
                .register(meterRegistry);

        this.writeResponses = Counter.builder("endpoint_write_responses")
                .description("Number of write responses")
                .register(meterRegistry);

        this.otherResponses = Counter.builder("endpoint_other_responses")
                .description("Number of other responses")
                .register(meterRegistry);

        // Initialize latency timers (Redis Enterprise style)
        this.readRequestsLatency = Timer.builder("endpoint_read_requests_latency")
                .description("Latency histogram of read commands")
                .register(meterRegistry);

        this.writeRequestsLatency = Timer.builder("endpoint_write_requests_latency")
                .description("Latency histogram of write commands")
                .register(meterRegistry);

        this.otherRequestsLatency = Timer.builder("endpoint_other_requests_latency")
                .description("Latency histogram of other commands")
                .register(meterRegistry);

        // Initialize legacy server-wide metrics
        this.totalCommandsProcessed = Counter.builder("redis_commands_total")
                .description("Total number of commands processed")
                .register(meterRegistry);

        this.totalErrors = Counter.builder("redis_errors_total")
                .description("Total number of errors encountered")
                .register(meterRegistry);

        // Initialize storage metrics (Redis Enterprise style)
        this.expiredKeys = Counter.builder("redis_expired_keys_total")
                .description("Total number of expired keys")
                .register(meterRegistry);

        this.evictedKeys = Counter.builder("redis_evicted_keys_total")
                .description("Total number of evicted keys")
                .register(meterRegistry);

        this.keysRead = Counter.builder("redis_keys_read_total")
                .description("Total number of keys read")
                .register(meterRegistry);

        this.keysWritten = Counter.builder("redis_keys_written_total")
                .description("Total number of keys written")
                .register(meterRegistry);

        this.keyspaceReadHits = Counter.builder("redis_keyspace_read_hits")
                .description("Number of read operations accessing an existing keyspace")
                .register(meterRegistry);

        this.keyspaceReadMisses = Counter.builder("redis_keyspace_read_misses")
                .description("Number of read operations accessing a non-existing keyspace")
                .register(meterRegistry);

        this.keyspaceWriteHits = Counter.builder("redis_keyspace_write_hits")
                .description("Number of write operations accessing an existing keyspace")
                .register(meterRegistry);

        this.keyspaceWriteMisses = Counter.builder("redis_keyspace_write_misses")
                .description("Number of write operations accessing a non-existing keyspace")
                .register(meterRegistry);

        // Initialize replication metrics (Redis Enterprise style)
        this.replicationCommandsSent = Counter.builder("redis_replication_commands_sent_total")
                .description("Total number of commands sent to replicas")
                .register(meterRegistry);

        // Initialize pub/sub metrics
        this.messagesPublished = Counter.builder("redis_pubsub_messages_published_total")
                .description("Total number of messages published")
                .register(meterRegistry);

        // Initialize transaction metrics
        this.transactionCommands = Counter.builder("redis_transaction_commands_total")
                .description("Total number of commands executed in transactions")
                .register(meterRegistry);

        this.failedTransactions = Counter.builder("redis_transactions_failed_total")
                .description("Total number of failed transactions")
                .register(meterRegistry);

        // Initialize persistence metrics
        this.rdbSaves = Counter.builder("redis_rdb_saves_total")
                .description("Total number of RDB save operations")
                .register(meterRegistry);

        this.aofWrites = Counter.builder("redis_aof_writes_total")
                .description("Total number of AOF write operations")
                .register(meterRegistry);

        this.persistenceErrors = Counter.builder("redis_persistence_errors_total")
                .description("Total number of persistence errors")
                .register(meterRegistry);

        // Initialize network metrics
        this.totalNetInputBytes = Counter.builder("redis_total_net_input_bytes")
                .description("Number of bytes received by the server")
                .register(meterRegistry);

        this.totalNetOutputBytes = Counter.builder("redis_total_net_output_bytes")
                .description("Number of bytes sent by the server")
                .register(meterRegistry);

        this.totalConnectionsReceived = Counter.builder("redis_total_connections_received")
                .description("Number of connections received by the server")
                .register(meterRegistry);

        // Register gauges for dynamic metrics
        registerGauges();

        LOGGER.info("MetricsCollector initialized at {}", startTime);
    }

    private void registerGauges() {
        // Active connections gauge
        Gauge.builder("redis_connected_clients", activeConnections, AtomicInteger::get)
                .description("Number of client connections")
                .register(meterRegistry);

        // Memory usage gauge
        Gauge.builder("redis_memory_usage_bytes", memoryUsage, AtomicLong::get)
                .description("Memory usage in bytes")
                .register(meterRegistry);

        // Uptime gauge
        Gauge.builder("redis_uptime_seconds", this, MetricsCollector::getUptimeSeconds)
                .description("Server uptime in seconds")
                .register(meterRegistry);

        // Replication lag gauge
        Gauge.builder("redis_replication_lag_milliseconds", replicationLag, AtomicLong::get)
                .description("Replication lag in milliseconds")
                .register(meterRegistry);

        // Replica connections gauge
        Gauge.builder("redis_connected_slaves", connectedReplicas, AtomicInteger::get)
                .description("Number of connected replicas")
                .register(meterRegistry);

        // Active channels gauge
        Gauge.builder("redis_pubsub_channels", activeChannels, AtomicInteger::get)
                .description("Number of active pub/sub channels")
                .register(meterRegistry);

        // Active transactions gauge
        Gauge.builder("redis_active_transactions", activeTransactions, AtomicInteger::get)
                .description("Number of active transactions")
                .register(meterRegistry);
    }

    // Server-wide metrics methods
    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    public void incrementTotalCommands() {
        totalCommandsProcessed.increment();
    }

    public void incrementTotalErrors() {
        totalErrors.increment();
    }

    public void setMemoryUsage(long bytes) {
        memoryUsage.set(bytes);
    }

    public double getActiveConnectionsCount() {
        return activeConnections.get();
    }

    public double getMemoryUsage() {
        return memoryUsage.get();
    }

    public double getUptimeSeconds() {
        return Duration.between(startTime, Instant.now()).getSeconds();
    }

    // Command metrics methods
    public void recordCommandExecution(String commandName, Duration duration) {
        getCommandCounter(commandName).increment();
        getCommandTimer(commandName).record(duration);
    }

    public void incrementCommandError(String commandName) {
        getCommandErrorCounter(commandName).increment();
    }

    private Counter getCommandCounter(String commandName) {
        return commandCounters.computeIfAbsent(commandName.toLowerCase(),
                name -> Counter.builder("redis_command_executions_total")
                        .tag("command", name)
                        .description("Number of executions for command: " + name)
                        .register(meterRegistry));
    }

    private Timer getCommandTimer(String commandName) {
        return commandTimers.computeIfAbsent(commandName.toLowerCase(),
                name -> Timer.builder("redis_command_duration_seconds")
                        .tag("command", name)
                        .description("Execution duration for command: " + name)
                        .register(meterRegistry));
    }

    private Counter getCommandErrorCounter(String commandName) {
        return commandErrors.computeIfAbsent(commandName.toLowerCase(),
                name -> Counter.builder("redis_command_errors_total")
                        .tag("command", name)
                        .description("Number of errors for command: " + name)
                        .register(meterRegistry));
    }

    // Storage metrics methods
    public void incrementKeyCount(String keyType) {
        keyCountByType.computeIfAbsent(keyType.toLowerCase(), type -> {
            AtomicInteger counter = new AtomicInteger(0);
            Gauge.builder("redis_keys_total", counter, AtomicInteger::get)
                    .tag("type", type)
                    .description("Number of keys by type: " + type)
                    .register(meterRegistry);
            return counter;
        }).incrementAndGet();
    }

    public void decrementKeyCount(String keyType) {
        AtomicInteger counter = keyCountByType.get(keyType.toLowerCase());
        if (counter != null) {
            counter.decrementAndGet();
        }
    }

    public void incrementExpiredKeys() {
        expiredKeys.increment();
    }

    public void incrementExpiredKeys(int count) {
        evictedKeys.increment(count);
    }

    public void incrementEvictedKeys() {
        evictedKeys.increment();
    }

    // Replication metrics methods
    public void setReplicationLag(long lagMillis) {
        replicationLag.set(lagMillis);
    }

    public void incrementReplicaConnections() {
        connectedReplicas.incrementAndGet();
    }

    public void decrementReplicaConnections() {
        connectedReplicas.decrementAndGet();
    }

    public void incrementReplicationCommandsSent() {
        replicationCommandsSent.increment();
    }

    public double getReplicationLag() {
        return replicationLag.get();
    }

    public double getReplicaConnections() {
        return connectedReplicas.get();
    }

    // Pub/Sub metrics methods
    public void incrementActiveChannels() {
        activeChannels.incrementAndGet();
    }

    public void decrementActiveChannels() {
        activeChannels.decrementAndGet();
    }

    public void incrementMessagesPublished() {
        messagesPublished.increment();
    }

    public void setSubscribersForChannel(String channel, int count) {
        subscribersPerChannel.computeIfAbsent(channel, ch -> {
            AtomicInteger counter = new AtomicInteger(0);
            Gauge.builder("redis_pubsub_subscribers", counter, AtomicInteger::get)
                    .tag("channel", ch)
                    .description("Number of subscribers for channel: " + ch)
                    .register(meterRegistry);
            return counter;
        }).set(count);
    }

    public double getActiveChannels() {
        return activeChannels.get();
    }

    // Transaction metrics methods
    public void incrementActiveTransactions() {
        activeTransactions.incrementAndGet();
    }

    public void decrementActiveTransactions() {
        activeTransactions.decrementAndGet();
    }

    public void incrementTransactionCommands() {
        transactionCommands.increment();
    }

    public void incrementFailedTransactions() {
        failedTransactions.increment();
    }

    public double getActiveTransactions() {
        return activeTransactions.get();
    }

    // Persistence metrics methods
    public void incrementRdbSaves() {
        rdbSaves.increment();
    }

    public void incrementAofWrites() {
        aofWrites.increment();
    }

    public void incrementPersistenceErrors() {
        persistenceErrors.increment();
    }

    // Metrics access methods
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public Map<String, Object> getAllMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();

        // Server metrics
        metrics.put("active_connections", activeConnections.get());
        metrics.put("total_commands_processed", totalCommandsProcessed.count());
        metrics.put("total_errors", totalErrors.count());
        metrics.put("memory_usage_bytes", memoryUsage.get());
        metrics.put("uptime_seconds", getUptimeSeconds());

        // Command metrics
        Map<String, Object> commandMetrics = new ConcurrentHashMap<>();
        commandCounters.forEach((cmd, counter) -> commandMetrics.put(cmd + "_count", counter.count()));
        commandErrors.forEach((cmd, counter) -> commandMetrics.put(cmd + "_errors", counter.count()));
        metrics.put("commands", commandMetrics);

        // Storage metrics
        Map<String, Object> storageMetrics = new ConcurrentHashMap<>();
        keyCountByType.forEach((type, counter) -> storageMetrics.put(type + "_keys", counter.get()));
        storageMetrics.put("expired_keys", expiredKeys.count());
        storageMetrics.put("evicted_keys", evictedKeys.count());
        metrics.put("storage", storageMetrics);

        // Replication metrics
        Map<String, Object> replicationMetrics = new ConcurrentHashMap<>();
        replicationMetrics.put("lag_milliseconds", replicationLag.get());
        replicationMetrics.put("connected_replicas", connectedReplicas.get());
        replicationMetrics.put("commands_sent", replicationCommandsSent.count());
        metrics.put("replication", replicationMetrics);

        // Pub/Sub metrics
        Map<String, Object> pubsubMetrics = new ConcurrentHashMap<>();
        pubsubMetrics.put("active_channels", activeChannels.get());
        pubsubMetrics.put("messages_published", messagesPublished.count());
        Map<String, Integer> subscriberCounts = new ConcurrentHashMap<>();
        subscribersPerChannel.forEach((channel, counter) -> subscriberCounts.put(channel, counter.get()));
        pubsubMetrics.put("subscribers_per_channel", subscriberCounts);
        metrics.put("pubsub", pubsubMetrics);

        // Transaction metrics
        Map<String, Object> transactionMetrics = new ConcurrentHashMap<>();
        transactionMetrics.put("active_transactions", activeTransactions.get());
        transactionMetrics.put("transaction_commands", transactionCommands.count());
        transactionMetrics.put("failed_transactions", failedTransactions.count());
        metrics.put("transactions", transactionMetrics);

        // Persistence metrics
        Map<String, Object> persistenceMetrics = new ConcurrentHashMap<>();
        persistenceMetrics.put("rdb_saves", rdbSaves.count());
        persistenceMetrics.put("aof_writes", aofWrites.count());
        persistenceMetrics.put("persistence_errors", persistenceErrors.count());
        metrics.put("persistence", persistenceMetrics);

        return metrics;
    }

    // ============ Redis Enterprise Compatible Metric Recording Methods
    // ============

    // Client Connection Metrics
    public void recordClientConnection() {
        clientConnections.increment();
        totalConnectionsReceived.increment();
        activeConnections.incrementAndGet();
    }

    public void recordClientDisconnection() {
        clientDisconnections.increment();
        activeConnections.decrementAndGet();
    }

    public void recordClientConnectionFailure() {
        clientConnectionFailures.increment();
    }

    // Command Type Metrics (Redis Enterprise style)
    public void recordReadCommand(String commandName, Duration latency) {
        readRequests.increment();
        readRequestsLatency.record(latency);
        recordCommandExecution(commandName, latency);
    }

    public void recordWriteCommand(String commandName, Duration latency) {
        writeRequests.increment();
        writeRequestsLatency.record(latency);
        recordCommandExecution(commandName, latency);
    }

    public void recordOtherCommand(String commandName, Duration latency) {
        otherRequests.increment();
        otherRequestsLatency.record(latency);
        recordCommandExecution(commandName, latency);
    }

    public void recordReadResponse() {
        readResponses.increment();
    }

    public void recordWriteResponse() {
        writeResponses.increment();
    }

    public void recordOtherResponse() {
        otherResponses.increment();
    }

    // Key Type Metrics
    public void recordKeyCreated(String keyType) {
        totalKeys.incrementAndGet();
        switch (keyType.toLowerCase()) {
            case "string" -> stringKeys.incrementAndGet();
            case "list" -> listKeys.incrementAndGet();
            case "set" -> setKeys.incrementAndGet();
            case "zset" -> zsetKeys.incrementAndGet();
            case "hash" -> hashKeys.incrementAndGet();
            case "stream" -> streamKeys.incrementAndGet();
        }
    }

    public void recordKeyDeleted(String keyType) {
        totalKeys.decrementAndGet();
        switch (keyType.toLowerCase()) {
            case "string" -> stringKeys.decrementAndGet();
            case "list" -> listKeys.decrementAndGet();
            case "set" -> setKeys.decrementAndGet();
            case "zset" -> zsetKeys.decrementAndGet();
            case "hash" -> hashKeys.decrementAndGet();
            case "stream" -> streamKeys.decrementAndGet();
        }
    }

    // Key Size Distribution Metrics
    public void recordStringSize(long sizeBytes) {
        long sizeMB = sizeBytes / (1024 * 1024);
        if (sizeMB < 128) {
            stringsUnder128M.incrementAndGet();
        } else if (sizeMB <= 512) {
            strings128MTo512M.incrementAndGet();
        } else {
            stringsOver512M.incrementAndGet();
        }
    }

    public void recordListSize(long elementCount) {
        if (elementCount < 1_000_000) {
            listsUnder1M.incrementAndGet();
        } else if (elementCount <= 8_000_000) {
            lists1MTo8M.incrementAndGet();
        } else {
            listsOver8M.incrementAndGet();
        }
    }

    public void recordSetSize(long elementCount) {
        if (elementCount < 1_000_000) {
            setsUnder1M.incrementAndGet();
        } else if (elementCount <= 8_000_000) {
            sets1MTo8M.incrementAndGet();
        } else {
            setsOver8M.incrementAndGet();
        }
    }

    public void recordZSetSize(long elementCount) {
        if (elementCount < 1_000_000) {
            zsetsUnder1M.incrementAndGet();
        } else if (elementCount <= 8_000_000) {
            zsets1MTo8M.incrementAndGet();
        } else {
            zsetsOver8M.incrementAndGet();
        }
    }

    // Keyspace Hit/Miss Metrics
    public void recordKeyspaceReadHit() {
        keyspaceReadHits.increment();
        keysRead.increment();
    }

    public void recordKeyspaceReadMiss() {
        keyspaceReadMisses.increment();
    }

    public void recordKeyspaceWriteHit() {
        keyspaceWriteHits.increment();
        keysWritten.increment();
    }

    public void recordKeyspaceWriteMiss() {
        keyspaceWriteMisses.increment();
        keysWritten.increment();
    }

    // Network Metrics
    public void recordNetworkInput(long bytes) {
        // Note: Counter.increment(double) adds the value to the counter
        totalNetInputBytes.increment(bytes);
    }

    public void recordNetworkOutput(long bytes) {
        totalNetOutputBytes.increment(bytes);
    }

    // Replication Metrics
    public void recordReplicationCommand() {
        replicationCommandsSent.increment();
    }

    public void updateMasterReplOffset(long offset) {
        masterReplOffset.set(offset);
    }

    public void setMasterSyncInProgress(boolean inProgress) {
        masterSyncInProgress.set(inProgress ? 1 : 0);
    }

    // Persistence Metrics
    public void recordRdbSave() {
        rdbSaves.increment();
    }

    public void recordAofWrite() {
        aofWrites.increment();
    }

    public void setRdbBgsaveInProgress(boolean inProgress) {
        rdbBgsaveInProgress.set(inProgress ? 1 : 0);
    }

    // Blocking Operations
    public void incrementBlockedClients() {
        blockedClients.incrementAndGet();
    }

    public void decrementBlockedClients() {
        blockedClients.decrementAndGet();
    }

    public int getTotalKeys() {
        return totalKeys.get();
    }

    public int getStringKeys() {
        return stringKeys.get();
    }

    public int getListKeys() {
        return listKeys.get();
    }

    public int getSetKeys() {
        return setKeys.get();
    }

    public int getZsetKeys() {
        return zsetKeys.get();
    }

    public int getHashKeys() {
        return hashKeys.get();
    }

    public int getStreamKeys() {
        return streamKeys.get();
    }

    public int getBlockedClients() {
        return blockedClients.get();
    }

    public long getMasterReplOffset() {
        return masterReplOffset.get();
    }

    public boolean isMasterSyncInProgress() {
        return masterSyncInProgress.get() == 1;
    }

    public boolean isRdbBgsaveInProgress() {
        return rdbBgsaveInProgress.get() == 1;
    }

    // Error recording method
    public void recordError() {
        totalErrors.increment();
    }

    // Counter getters for endpoint metrics
    public double getClientConnections() {
        return clientConnections.count();
    }

    public double getClientDisconnections() {
        return clientDisconnections.count();
    }

    public double getClientConnectionFailures() {
        return clientConnectionFailures.count();
    }

    public double getReadRequests() {
        return readRequests.count();
    }

    public double getWriteRequests() {
        return writeRequests.count();
    }

    public double getOtherRequests() {
        return otherRequests.count();
    }

    public double getReadResponses() {
        return readResponses.count();
    }

    public double getWriteResponses() {
        return writeResponses.count();
    }

    public double getOtherResponses() {
        return otherResponses.count();
    }

    public double getKeyspaceReadHits() {
        return keyspaceReadHits.count();
    }

    public double getKeyspaceReadMisses() {
        return keyspaceReadMisses.count();
    }

    public double getKeyspaceWriteHits() {
        return keyspaceWriteHits.count();
    }

    public double getKeyspaceWriteMisses() {
        return keyspaceWriteMisses.count();
    }

    public double getTotalNetInputBytes() {
        return totalNetInputBytes.count();
    }

    public double getTotalNetOutputBytes() {
        return totalNetOutputBytes.count();
    }

    public double getTotalConnectionsReceived() {
        return totalConnectionsReceived.count();
    }
}
