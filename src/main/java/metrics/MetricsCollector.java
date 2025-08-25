package metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central metrics collector for Redis server implementation.
 * Collects and tracks various metrics including server-wide, command-specific,
 * storage, replication, pub/sub, transaction, and persistence metrics.
 */
public final class MetricsCollector {
    private static final Logger log = LoggerFactory.getLogger(MetricsCollector.class);
    
    private final MeterRegistry meterRegistry;
    private final Instant startTime;
    
    // Server-wide metrics
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final Counter totalCommandsProcessed;
    private final Counter totalErrors;
    private final AtomicLong memoryUsage = new AtomicLong(0);
    
    // Command metrics
    private final Map<String, Counter> commandCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> commandTimers = new ConcurrentHashMap<>();
    private final Map<String, Counter> commandErrors = new ConcurrentHashMap<>();
    
    // Storage metrics
    private final Map<String, AtomicInteger> keyCountByType = new ConcurrentHashMap<>();
    private final Counter expiredKeys;
    private final Counter evictedKeys;
    
    // Replication metrics
    private final AtomicLong replicationLag = new AtomicLong(0);
    private final AtomicInteger replicaConnections = new AtomicInteger(0);
    private final Counter replicationCommandsSent;
    
    // Pub/Sub metrics
    private final AtomicInteger activeChannels = new AtomicInteger(0);
    private final Counter messagesPublished;
    private final Map<String, AtomicInteger> subscribersPerChannel = new ConcurrentHashMap<>();
    
    // Transaction metrics
    private final AtomicInteger activeTransactions = new AtomicInteger(0);
    private final Counter transactionCommands;
    private final Counter failedTransactions;
    
    // Persistence metrics
    private final Counter rdbSaves;
    private final Counter aofWrites;
    private final Counter persistenceErrors;
    
    public MetricsCollector() {
        this.startTime = Instant.now();
        this.meterRegistry = new SimpleMeterRegistry();
        
        // Initialize server-wide metrics
        this.totalCommandsProcessed = Counter.builder("redis_commands_total")
                .description("Total number of commands processed")
                .register(meterRegistry);
        
        this.totalErrors = Counter.builder("redis_errors_total")
                .description("Total number of errors encountered")
                .register(meterRegistry);
        
        // Initialize storage metrics
        this.expiredKeys = Counter.builder("redis_expired_keys_total")
                .description("Total number of expired keys")
                .register(meterRegistry);
        
        this.evictedKeys = Counter.builder("redis_evicted_keys_total")
                .description("Total number of evicted keys")
                .register(meterRegistry);
        
        // Initialize replication metrics
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
        
        // Register gauges for dynamic metrics
        registerGauges();
        
        log.info("MetricsCollector initialized at {}", startTime);
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
        Gauge.builder("redis_connected_slaves", replicaConnections, AtomicInteger::get)
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
        return commandCounters.computeIfAbsent(commandName.toLowerCase(), name ->
                Counter.builder("redis_command_executions_total")
                        .tag("command", name)
                        .description("Number of executions for command: " + name)
                        .register(meterRegistry));
    }
    
    private Timer getCommandTimer(String commandName) {
        return commandTimers.computeIfAbsent(commandName.toLowerCase(), name ->
                Timer.builder("redis_command_duration_seconds")
                        .tag("command", name)
                        .description("Execution duration for command: " + name)
                        .register(meterRegistry));
    }
    
    private Counter getCommandErrorCounter(String commandName) {
        return commandErrors.computeIfAbsent(commandName.toLowerCase(), name ->
                Counter.builder("redis_command_errors_total")
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
    
    public void incrementEvictedKeys() {
        evictedKeys.increment();
    }
    
    // Replication metrics methods
    public void setReplicationLag(long lagMillis) {
        replicationLag.set(lagMillis);
    }
    
    public void incrementReplicaConnections() {
        replicaConnections.incrementAndGet();
    }
    
    public void decrementReplicaConnections() {
        replicaConnections.decrementAndGet();
    }
    
    public void incrementReplicationCommandsSent() {
        replicationCommandsSent.increment();
    }
    
    public double getReplicationLag() {
        return replicationLag.get();
    }
    
    public double getReplicaConnections() {
        return replicaConnections.get();
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
        commandCounters.forEach((cmd, counter) -> 
            commandMetrics.put(cmd + "_count", counter.count()));
        commandErrors.forEach((cmd, counter) -> 
            commandMetrics.put(cmd + "_errors", counter.count()));
        metrics.put("commands", commandMetrics);
        
        // Storage metrics
        Map<String, Object> storageMetrics = new ConcurrentHashMap<>();
        keyCountByType.forEach((type, counter) -> 
            storageMetrics.put(type + "_keys", counter.get()));
        storageMetrics.put("expired_keys", expiredKeys.count());
        storageMetrics.put("evicted_keys", evictedKeys.count());
        metrics.put("storage", storageMetrics);
        
        // Replication metrics
        Map<String, Object> replicationMetrics = new ConcurrentHashMap<>();
        replicationMetrics.put("lag_milliseconds", replicationLag.get());
        replicationMetrics.put("connected_replicas", replicaConnections.get());
        replicationMetrics.put("commands_sent", replicationCommandsSent.count());
        metrics.put("replication", replicationMetrics);
        
        // Pub/Sub metrics
        Map<String, Object> pubsubMetrics = new ConcurrentHashMap<>();
        pubsubMetrics.put("active_channels", activeChannels.get());
        pubsubMetrics.put("messages_published", messagesPublished.count());
        Map<String, Integer> subscriberCounts = new ConcurrentHashMap<>();
        subscribersPerChannel.forEach((channel, counter) -> 
            subscriberCounts.put(channel, counter.get()));
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
}
