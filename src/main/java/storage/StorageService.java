package storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import collections.QuickZSet;
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

    public StorageService() {
        this.stringRepo = new StringRepository(store);
        this.listRepo = new ListRepository(store);
        this.streamRepo = new StreamRepository(store);
        this.zSetRepo = new ZSetRepository(store);
    }

    public Map<String, StoredValue<?>> getStore() {
        return store;
    }

    // String operations
    public void setString(String key, String value, ExpiryPolicy expiry) {
        stringRepo.put(key, value, expiry);
    }

    public Optional<String> getString(String key) {
        return stringRepo.get(key);
    }

    public long incrementString(String key) {
        return stringRepo.increment(key);
    }

    // List operations
    public int leftPush(String key, String... values) {
        return listRepo.pushLeft(key, values);
    }

    public int rightPush(String key, String... values) {
        return listRepo.pushRight(key, values);
    }

    public Optional<String> leftPop(String key) {
        return listRepo.popLeft(key);
    }

    public Optional<String> rightPop(String key) {
        return listRepo.popRight(key);
    }

    public List<String> leftPop(String key, int count) {
        return listRepo.popLeft(key, count);
    }

    public List<String> rightPop(String key, int count) {
        return listRepo.popRight(key, count);
    }

    public List<String> getListRange(String key, int start, int end) {
        return listRepo.range(key, start, end);
    }

    public int getListLength(String key) {
        return listRepo.getLength(key);
    }

    // Stream operations
    public String addStreamEntry(String key, String id, Map<String, String> fields,
            ExpiryPolicy expiry) {
        return streamRepo.addEntry(key, id, fields, expiry);
    }

    public Optional<String> getLastStreamId(String key) {
        return streamRepo.getLastId(key);
    }

    public List<StreamRangeEntry> getStreamRange(String key, String start, String end, int count) {
        return streamRepo.getRange(key, start, end, count);
    }

    public List<StreamRangeEntry> getStreamRange(String key, String start, String end) {
        return streamRepo.getRange(key, start, end, 0);
    }

    public List<StreamRangeEntry> getStreamAfter(String key, String afterId, int count) {
        return streamRepo.getAfter(key, afterId, count);
    }

    // === ZSet operations ===

    /** Add or update a member with a score */
    public void zAdd(String key, String member, double score) {
        zSetRepo.add(key, member, score);
    }

    /** Remove a member */
    public boolean zRemove(String key, String member) {
        return zSetRepo.remove(key, member);
    }

    /** Pop member with smallest score */
    public Optional<QuickZSet.ZSetEntry> zPopMin(String key) {
        return zSetRepo.popMin(key);
    }

    /** Pop member with largest score */
    public Optional<QuickZSet.ZSetEntry> zPopMax(String key) {
        return zSetRepo.popMax(key);
    }

    /** Get range by rank (supports negative indices) */
    public List<QuickZSet.ZSetEntry> zRange(String key, int start, int end) {
        return zSetRepo.range(key, start, end);
    }

    /** Get range by score */
    public List<QuickZSet.ZSetEntry> zRangeByScore(String key, double min, double max) {
        return zSetRepo.rangeByScore(key, min, max);
    }

    /** Get size */
    public int zSize(String key) {
        return zSetRepo.size(key);
    }

    /** Get score for member */
    public Double zScore(String key, String member) {
        return zSetRepo.getScore(key, member);
    }

    /** Get rank for member */
    public Long zRank(String key, String member) {
        return zSetRepo.getRank(key, member);
    }

    // General operations
    public boolean exists(String key) {
        return getValidValue(key) != null;
    }

    public boolean delete(String key) {
        return store.remove(key) != null;
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
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
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
