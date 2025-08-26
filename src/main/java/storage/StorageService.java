package storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import collections.QuickZSet;
import events.EventPublisher;
import storage.expiry.ExpiryPolicy;
import storage.repositories.ListRepository;
import storage.repositories.StreamRepository;
import storage.repositories.StringRepository;
import storage.repositories.ZSetRepository;
import storage.types.StoredValue;
import storage.types.ValueType;
import storage.types.streams.StreamRangeEntry;

public final class StorageService {
    private final Map<String, StoredValue<?>> store = new ConcurrentHashMap<>();
    private final StringRepository stringRepo;
    private final ListRepository listRepo;
    private final StreamRepository streamRepo;
    private final ZSetRepository zSetRepo;
    private EventPublisher eventPublisher;

    public StorageService() {
        this.stringRepo = new StringRepository(store);
        this.listRepo = new ListRepository(store);
        this.streamRepo = new StreamRepository(store);
        this.zSetRepo = new ZSetRepository(store);
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private void notifyKeyModified(String key) {
        if (eventPublisher != null) {
            eventPublisher.publishKeyModified(key);
        }
    }

    private void recordReadMetrics(boolean hasResult) {
        if (eventPublisher instanceof server.ServerContext serverContext) {
            if (hasResult) {
                serverContext.getMetricsCollector().recordKeyspaceReadHit();
            } else {
                serverContext.getMetricsCollector().recordKeyspaceReadMiss();
            }
        }
    }

    private void recordWriteMetrics(boolean isNewKey, String keyType) {
        if (eventPublisher instanceof server.ServerContext serverContext) {
            if (isNewKey) {
                serverContext.getMetricsCollector().recordKeyspaceWriteMiss();
                serverContext.getMetricsCollector().recordKeyCreated(keyType);
            } else {
                serverContext.getMetricsCollector().recordKeyspaceWriteHit();
            }
        }
    }

    public Map<String, StoredValue<?>> getStore() {
        return store;
    }

    // String operations
    public void setString(String key, String value, ExpiryPolicy expiry) {
        boolean isNewKey = !stringRepo.exists(key);
        stringRepo.put(key, value, expiry);
        recordWriteMetrics(isNewKey, "string");
        notifyKeyModified(key);
    }

    public Optional<String> getString(String key) {
        Optional<String> result = stringRepo.get(key);
        recordReadMetrics(result.isPresent());
        return result;
    }

    public long incrementString(String key) {
        long result = stringRepo.increment(key);
        notifyKeyModified(key);
        return result;
    }

    // List operations
    public int leftPush(String key, String... values) {
        boolean isNewKey = !listRepo.exists(key);
        int result = listRepo.pushLeft(key, values);
        recordWriteMetrics(isNewKey, "list");
        notifyKeyModified(key);
        return result;
    }

    public int rightPush(String key, String... values) {
        boolean isNewKey = !listRepo.exists(key);
        int result = listRepo.pushRight(key, values);
        recordWriteMetrics(isNewKey, "list");
        notifyKeyModified(key);
        return result;
    }

    public Optional<String> leftPop(String key) {
        Optional<String> result = listRepo.popLeft(key);
        recordReadMetrics(result.isPresent());
        if (result.isPresent()) {
            notifyKeyModified(key);
        }
        return result;
    }

    public Optional<String> rightPop(String key) {
        Optional<String> result = listRepo.popRight(key);
        recordReadMetrics(result.isPresent());
        if (result.isPresent()) {
            notifyKeyModified(key);
        }
        return result;
    }

    public List<String> leftPop(String key, int count) {
        List<String> result = listRepo.popLeft(key, count);
        if (!result.isEmpty()) {
            notifyKeyModified(key);
        }
        return result;
    }

    public List<String> rightPop(String key, int count) {
        List<String> result = listRepo.popRight(key, count);
        if (!result.isEmpty()) {
            notifyKeyModified(key);
        }
        return result;
    }

    public List<String> getListRange(String key, int start, int end) {
        List<String> result = listRepo.range(key, start, end);
        recordReadMetrics(!result.isEmpty());
        return result;
    }

    public int getListLength(String key) {
        int result = listRepo.getLength(key);
        recordReadMetrics(result > 0);
        return result;
    }

    // Stream operations
    public String addStreamEntry(String key, String id, Map<String, String> fields,
            ExpiryPolicy expiry) {
        boolean isNewKey = !streamRepo.exists(key);
        String result = streamRepo.addEntry(key, id, fields, expiry);
        recordWriteMetrics(isNewKey, "stream");
        notifyKeyModified(key);
        return result;
    }

    public Optional<String> getLastStreamId(String key) {
        Optional<String> result = streamRepo.getLastId(key);
        recordReadMetrics(result.isPresent());
        return result;
    }

    public List<StreamRangeEntry> getStreamRange(String key, String start, String end, int count) {
        List<StreamRangeEntry> result = streamRepo.getRange(key, start, end, count);
        recordReadMetrics(!result.isEmpty());
        return result;
    }

    public List<StreamRangeEntry> getStreamRange(String key, String start, String end) {
        List<StreamRangeEntry> result = streamRepo.getRange(key, start, end, 0);
        recordReadMetrics(!result.isEmpty());
        return result;
    }

    public List<StreamRangeEntry> getStreamAfter(String key, String afterId, int count) {
        List<StreamRangeEntry> result = streamRepo.getAfter(key, afterId, count);
        recordReadMetrics(!result.isEmpty());
        return result;
    }

    // === ZSet operations ===

    /** Add or update a member with a score, returns true if new member was added */
    public boolean zAdd(String key, String member, double score) {
        boolean isNewKey = !zSetRepo.exists(key);
        boolean result = zSetRepo.add(key, member, score);
        recordWriteMetrics(isNewKey, "zset");
        notifyKeyModified(key);
        return result;
    }

    /** Remove a member */
    public boolean zRemove(String key, String member) {
        boolean result = zSetRepo.remove(key, member);
        if (result) {
            notifyKeyModified(key);
        }
        return result;
    }

    /** Pop member with smallest score */
    public Optional<QuickZSet.ZSetEntry> zPopMin(String key) {
        Optional<QuickZSet.ZSetEntry> result = zSetRepo.popMin(key);
        recordReadMetrics(result.isPresent());
        if (result.isPresent()) {
            notifyKeyModified(key);
        }
        return result;
    }

    /** Pop member with largest score */
    public Optional<QuickZSet.ZSetEntry> zPopMax(String key) {
        Optional<QuickZSet.ZSetEntry> result = zSetRepo.popMax(key);
        recordReadMetrics(result.isPresent());
        if (result.isPresent()) {
            notifyKeyModified(key);
        }
        return result;
    }

    /** Get range by rank (supports negative indices) */
    public List<QuickZSet.ZSetEntry> zRange(String key, int start, int end) {
        List<QuickZSet.ZSetEntry> result = zSetRepo.range(key, start, end);
        recordReadMetrics(!result.isEmpty());
        return result;
    }

    /** Get range by score */
    public List<QuickZSet.ZSetEntry> zRangeByScore(String key, double min, double max) {
        List<QuickZSet.ZSetEntry> result = zSetRepo.rangeByScore(key, min, max);
        recordReadMetrics(!result.isEmpty());
        return result;
    }

    /** Get size */
    public int zSize(String key) {
        int result = zSetRepo.size(key);
        recordReadMetrics(result > 0);
        return result;
    }

    /** Get score for member */
    public Double zScore(String key, String member) {
        Double result = zSetRepo.getScore(key, member);
        recordReadMetrics(result != null);
        return result;
    }

    /** Get rank for member */
    public Long zRank(String key, String member) {
        Long result = zSetRepo.getRank(key, member);
        recordReadMetrics(result != null);
        return result;
    }

    // General operations
    public boolean exists(String key) {
        return getValidValue(key) != null;
    }

    public boolean delete(String key) {
        StoredValue<?> deletedValue = store.remove(key);
        if (deletedValue != null) {
            // Update metrics for deleted key type
            if (eventPublisher != null && eventPublisher instanceof server.ServerContext) {
                var metricsCollector = ((server.ServerContext) eventPublisher).getMetricsCollector();
                switch (deletedValue.type()) {
                    case STRING:
                        metricsCollector.decrementKeyCount("string");
                        break;
                    case LIST:
                        metricsCollector.decrementKeyCount("list");
                        break;
                    case STREAM:
                        metricsCollector.decrementKeyCount("stream");
                        break;
                    case ZSET:
                        metricsCollector.decrementKeyCount("zset");
                        break;
                    default:
                        break;
                }
            }
            notifyKeyModified(key);
            return true;
        }
        return false;
    }

    public ValueType getType(String key) {
        var value = getValidValue(key);
        return value != null ? value.type() : ValueType.NONE;
    }

    public List<String> getKeysByPattern(String pattern) {
        String regex = "^" + pattern
                .replace("?", ".")
                .replace("*", ".*")
                + "$";

        return store.keySet().stream()
                .filter(key -> key.matches(regex))
                .toList();
    }

    public void clear() {
        store.clear();
    }

    public void cleanup() {
        long expiredCount = store.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired())
                .count();
        
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // Record expired keys metrics
        if (expiredCount > 0 && eventPublisher instanceof server.ServerContext serverContext) {
            for (int i = 0; i < expiredCount; i++) {
                serverContext.getMetricsCollector().incrementExpiredKeys();
            }
        }
    }

    private StoredValue<?> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            return null;
        }
        return value;
    }

}
