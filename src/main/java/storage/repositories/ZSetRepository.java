package storage.repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import collections.QuickZSet;
import collections.QuickZSet.ZSetEntry;
import storage.Repository;
import storage.expiry.ExpiryPolicy;
import storage.types.StoredValue;
import storage.types.ValueType;
import storage.types.ZSetValue;

public final class ZSetRepository implements Repository<QuickZSet> {

    private final Map<String, StoredValue<?>> store;

    public ZSetRepository(Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void put(String key, QuickZSet value, ExpiryPolicy expiry) {
        store.put(key, new ZSetValue(value, expiry));
    }

    @Override
    public Optional<QuickZSet> get(String key) {
        return getValidValue(key)
                .filter(v -> v.type() == ValueType.ZSET)
                .map(v -> ((ZSetValue) v).value());
    }

    @Override
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public boolean exists(String key) {
        return getValidValue(key).isPresent();
    }

    @Override
    public ValueType getType(String key) {
        return getValidValue(key).map(StoredValue::type).orElse(ValueType.NONE);
    }

    private Optional<StoredValue<?>> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            value = null;
        }
        return Optional.ofNullable(value);
    }

    // === ZSet operations ===

    /** Add or update a member with a score, returns true if new member was added */
    public boolean add(String key, String member, double score) {
        QuickZSet zset = getOrCreate(key);
        return zset.add(member, score);
    }

    /** Remove a member */
    public boolean remove(String key, String member) {
        return get(key).map(zset -> zset.remove(member)).orElse(false);
    }

    /** Pop min */
    public Optional<ZSetEntry> popMin(String key) {
        return get(key).flatMap(QuickZSet::popMin);
    }

    public Optional<ZSetEntry> popMax(String key) {
        return get(key).flatMap(QuickZSet::popMax);
    }

    /** Range by rank (supports negative index) */
    public List<ZSetEntry> range(String key, int start, int end) {
        return get(key).map(zset -> zset.range(start, end)).orElse(List.of());
    }

    /** Range by score */
    public List<ZSetEntry> rangeByScore(String key, double min, double max) {
        return get(key).map(zset -> zset.rangeByScore(min, max)).orElse(List.of());
    }

    /** Size */
    public int size(String key) {
        return get(key).map(QuickZSet::size).orElse(0);
    }

    /** Get score for member */
    public Double getScore(String key, String member) {
        return get(key).map(zset -> zset.getScore(member)).orElse(null);
    }

    /** Get rank for member */
    public Long getRank(String key, String member) {
        return get(key).map(zset -> zset.getRank(member)).orElse(null);
    }

    /** Get or create a QuickZSet */
    private QuickZSet getOrCreate(String key) {
        return get(key).orElseGet(() -> {
            QuickZSet newSet = new QuickZSet();
            put(key, newSet);
            return newSet;
        });
    }
}
