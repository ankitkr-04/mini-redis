package storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * StorageService provides a unified interface for working with
 * multiple data structures (String, List, Stream, ZSet) in memory.
 * <p>
 * It also integrates with event publishers and metrics collectors
 * for keyspace operations.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

    // Key type labels for metrics
    private static final String TYPE_STRING = "string";
    private static final String TYPE_LIST = "list";
    private static final String TYPE_STREAM = "stream";
    private static final String TYPE_ZSET = "zset";
    // private static final String TYPE_HASH = "hash";
    // private static final String TYPE_SET = "set";

    private final Map<String, StoredValue<?>> store = new ConcurrentHashMap<>();
    private final StringRepository stringRepository;
    private final ListRepository listRepository;
    private final StreamRepository streamRepository;
    private final ZSetRepository zSetRepository;

    private EventPublisher eventPublisher;

    public StorageService() {
        this.stringRepository = new StringRepository(store);
        this.listRepository = new ListRepository(store);
        this.streamRepository = new StreamRepository(store);
        this.zSetRepository = new ZSetRepository(store);
    }

    public void setEventPublisher(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private void notifyKeyModified(final String key) {
        if (eventPublisher != null) {
            eventPublisher.publishKeyModified(key);
        }
    }

    // Performance optimized metrics - disable read metrics for high performance
    private static final boolean ENABLE_READ_METRICS = false; // Disabled for performance
    private static final boolean ENABLE_WRITE_METRICS = true; // Keep for important write tracking

    private void recordReadMetrics(final boolean hasResult) {
        if (ENABLE_READ_METRICS && eventPublisher instanceof final server.ServerContext serverContext) {
            if (hasResult) {
                serverContext.getMetricsCollector().recordKeyspaceReadHit();
            } else {
                serverContext.getMetricsCollector().recordKeyspaceReadMiss();
            }
        }
    }

    private void recordWriteMetrics(final boolean isNewKey, final String keyType) {
        if (ENABLE_WRITE_METRICS && eventPublisher instanceof final server.ServerContext serverContext) {
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

    /* ---------- String operations ---------- */

    public void setString(final String key, final String value, final ExpiryPolicy expiry) {
        final boolean isNewKey = !stringRepository.exists(key);
        stringRepository.put(key, value, expiry);
        recordWriteMetrics(isNewKey, TYPE_STRING);
        notifyKeyModified(key);
    }

    public Optional<String> getString(final String key) {
        final Optional<String> value = stringRepository.get(key);
        recordReadMetrics(value.isPresent());
        return value;
    }

    public long incrementString(final String key) {
        final long newValue = stringRepository.increment(key);
        notifyKeyModified(key);
        return newValue;
    }

    public long decrementString(final String key) {
        final long newValue = stringRepository.decrement(key);
        notifyKeyModified(key);
        return newValue;
    }

    /* ---------- List operations ---------- */

    public int leftPush(final String key, final String... values) {
        final boolean isNewKey = !listRepository.exists(key);
        final int newSize = listRepository.pushLeft(key, values);
        recordWriteMetrics(isNewKey, TYPE_LIST);
        notifyKeyModified(key);
        return newSize;
    }

    public int rightPush(final String key, final String... values) {
        final boolean isNewKey = !listRepository.exists(key);
        final int newSize = listRepository.pushRight(key, values);
        recordWriteMetrics(isNewKey, TYPE_LIST);
        notifyKeyModified(key);
        return newSize;
    }

    public Optional<String> leftPop(final String key) {
        final Optional<String> poppedValue = listRepository.popLeft(key);
        recordReadMetrics(poppedValue.isPresent());
        poppedValue.ifPresent(v -> notifyKeyModified(key));
        return poppedValue;
    }

    public Optional<String> rightPop(final String key) {
        final Optional<String> poppedValue = listRepository.popRight(key);
        recordReadMetrics(poppedValue.isPresent());
        poppedValue.ifPresent(v -> notifyKeyModified(key));
        return poppedValue;
    }

    public List<String> leftPop(final String key, final int count) {
        final List<String> poppedValues = listRepository.popLeft(key, count);
        if (!poppedValues.isEmpty()) {
            notifyKeyModified(key);
        }
        return poppedValues;
    }

    public List<String> rightPop(final String key, final int count) {
        final List<String> poppedValues = listRepository.popRight(key, count);
        if (!poppedValues.isEmpty()) {
            notifyKeyModified(key);
        }
        return poppedValues;
    }

    public List<String> getListRange(final String key, final int start, final int end) {
        final List<String> listSlice = listRepository.range(key, start, end);
        recordReadMetrics(!listSlice.isEmpty());
        return listSlice;
    }

    public int getListLength(final String key) {
        final int length = listRepository.getLength(key);
        recordReadMetrics(length > 0);
        return length;
    }

    /* ---------- Stream operations ---------- */

    public String addStreamEntry(final String key, final String id, final Map<String, String> fields,
            final ExpiryPolicy expiry) {
        final boolean isNewKey = !streamRepository.exists(key);
        final String newId = streamRepository.addEntry(key, id, fields, expiry);
        recordWriteMetrics(isNewKey, TYPE_STREAM);
        notifyKeyModified(key);
        return newId;
    }

    public Optional<String> getLastStreamId(final String key) {
        final Optional<String> lastId = streamRepository.getLastId(key);
        recordReadMetrics(lastId.isPresent());
        return lastId;
    }

    public List<StreamRangeEntry> getStreamRange(final String key, final String start, final String end,
            final int count) {
        final List<StreamRangeEntry> entries = streamRepository.getRange(key, start, end, count);
        recordReadMetrics(!entries.isEmpty());
        return entries;
    }

    public List<StreamRangeEntry> getStreamRange(final String key, final String start, final String end) {
        final List<StreamRangeEntry> entries = streamRepository.getRange(key, start, end, 0);
        recordReadMetrics(!entries.isEmpty());
        return entries;
    }

    public List<StreamRangeEntry> getStreamAfter(final String key, final String afterId, final int count) {
        final List<StreamRangeEntry> entries = streamRepository.getAfter(key, afterId, count);
        recordReadMetrics(!entries.isEmpty());
        return entries;
    }

    /* ---------- ZSet operations ---------- */

    public boolean zAdd(final String key, final String member, final double score) {
        final boolean isNewKey = !zSetRepository.exists(key);
        final boolean added = zSetRepository.add(key, member, score);
        recordWriteMetrics(isNewKey, TYPE_ZSET);
        notifyKeyModified(key);
        return added;
    }

    public boolean zRemove(final String key, final String member) {
        final boolean removed = zSetRepository.remove(key, member);
        if (removed) {
            notifyKeyModified(key);
        }
        return removed;
    }

    public Optional<QuickZSet.ZSetEntry> zPopMin(final String key) {
        final Optional<QuickZSet.ZSetEntry> minEntry = zSetRepository.popMin(key);
        recordReadMetrics(minEntry.isPresent());
        minEntry.ifPresent(v -> notifyKeyModified(key));
        return minEntry;
    }

    public Optional<QuickZSet.ZSetEntry> zPopMax(final String key) {
        final Optional<QuickZSet.ZSetEntry> maxEntry = zSetRepository.popMax(key);
        recordReadMetrics(maxEntry.isPresent());
        maxEntry.ifPresent(v -> notifyKeyModified(key));
        return maxEntry;
    }

    public List<QuickZSet.ZSetEntry> zRange(final String key, final int start, final int end) {
        final List<QuickZSet.ZSetEntry> entries = zSetRepository.range(key, start, end);
        recordReadMetrics(!entries.isEmpty());
        return entries;
    }

    public List<QuickZSet.ZSetEntry> zRangeByScore(final String key, final double min, final double max) {
        final List<QuickZSet.ZSetEntry> entries = zSetRepository.rangeByScore(key, min, max);
        recordReadMetrics(!entries.isEmpty());
        return entries;
    }

    public int zSize(final String key) {
        final int size = zSetRepository.size(key);
        recordReadMetrics(size > 0);
        return size;
    }

    public Double zScore(final String key, final String member) {
        final Double score = zSetRepository.getScore(key, member);
        recordReadMetrics(score != null);
        return score;
    }

    public Long zRank(final String key, final String member) {
        final Long rank = zSetRepository.getRank(key, member);
        recordReadMetrics(rank != null);
        return rank;
    }

    /* ---------- General operations ---------- */

    public boolean exists(final String key) {
        return getValidValue(key) != null;
    }

    public boolean delete(final String key) {
        final StoredValue<?> removedValue = store.remove(key);
        if (removedValue != null) {
            updateDeleteMetrics(removedValue);
            notifyKeyModified(key);
            return true;
        }
        return false;
    }

    public ValueType getType(final String key) {
        final StoredValue<?> value = getValidValue(key);
        return value != null ? value.type() : ValueType.NONE;
    }

    public List<String> getKeysByPattern(final String pattern) {
        final String regex = "^" + pattern.replace("?", ".").replace("*", ".*") + "$";
        return store.keySet().stream()
                .filter(key -> key.matches(regex))
                .toList();
    }

    public void clear() {
        store.values().forEach(this::updateDeleteMetrics);
        store.clear();
        if (eventPublisher != null) {
            eventPublisher.publishStoreCleared(); // optional event
        }
    }

    public void purgeExpiredKeys() {
        final long expiredCount = store.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired())
                .count();

        store.entrySet().removeIf(entry -> entry.getValue().isExpired());

        if (expiredCount > 0 && eventPublisher instanceof final server.ServerContext serverContext) {
            for (int i = 0; i < expiredCount; i++) {
                serverContext.getMetricsCollector().incrementExpiredKeys();
            }
        }
    }

    private StoredValue<?> getValidValue(final String key) {
        final StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            return null;
        }
        return value;
    }

    private void updateDeleteMetrics(final StoredValue<?> deletedValue) {
        if (eventPublisher instanceof final server.ServerContext serverContext) {
            final var metricsCollector = serverContext.getMetricsCollector();
            switch (deletedValue.type()) {
                case STRING -> metricsCollector.decrementKeyCount(TYPE_STRING);
                case LIST -> metricsCollector.decrementKeyCount(TYPE_LIST);
                case STREAM -> metricsCollector.decrementKeyCount(TYPE_STREAM);
                case ZSET -> metricsCollector.decrementKeyCount(TYPE_ZSET);
                // case HASH -> metricsCollector.decrementKeyCount(TYPE_HASH);
                // case SET -> metricsCollector.decrementKeyCount(TYPE_SET);
                default -> {
                    LOGGER.warn("Unknown value type for metrics update: {}", deletedValue.type());

                }
            }
        }
    }
}
