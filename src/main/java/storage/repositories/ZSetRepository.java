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

    public ZSetRepository(final Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void put(final String key, final QuickZSet value, final ExpiryPolicy expiry) {
        store.put(key, new ZSetValue(value, expiry));
    }

    @Override
    public Optional<QuickZSet> get(final String key) {
        return getValidValue(key)
                .filter(v -> v.type() == ValueType.ZSET)
                .map(v -> ((ZSetValue) v).value());
    }

    @Override
    public boolean delete(final String key) {
        return store.remove(key) != null;
    }

    @Override
    public boolean exists(final String key) {
        return getValidValue(key).isPresent();
    }

    @Override
    public ValueType getType(final String key) {
        return getValidValue(key).map(StoredValue::type).orElse(ValueType.NONE);
    }

    private Optional<StoredValue<?>> getValidValue(final String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            value = null;
        }
        return Optional.ofNullable(value);
    }

    // === ZSet operations ===

    /** Add or update a member with a score, returns true if new member was added */
    public boolean add(final String key, final String member, final double score) {
        final QuickZSet zset = getOrCreate(key);
        return zset.add(member, score);
    }

    /** Remove a member */
    public boolean remove(final String key, final String member) {
        return get(key).map(zset -> zset.remove(member)).orElse(false);
    }

    /** Pop min */
    public Optional<ZSetEntry> popMin(final String key) {
        return get(key).flatMap(QuickZSet::popMin);
    }

    public Optional<ZSetEntry> popMax(final String key) {
        return get(key).flatMap(QuickZSet::popMax);
    }

    /** Range by rank (supports negative index) */
    public List<ZSetEntry> range(final String key, final int start, final int end) {
        return get(key).map(zset -> zset.range(start, end)).orElse(List.of());
    }

    /** Range by score */
    public List<ZSetEntry> rangeByScore(final String key, final double min, final double max) {
        return get(key).map(zset -> zset.rangeByScore(min, max)).orElse(List.of());
    }

    /** Size */
    public int size(final String key) {
        return get(key).map(QuickZSet::size).orElse(0);
    }

    /** Get score for member */
    public Double getScore(final String key, final String member) {
        return get(key).map(zset -> zset.getScore(member)).orElse(null);
    }

    /** Get rank for member */
    public Long getRank(final String key, final String member) {
        return get(key).map(zset -> zset.getRank(member)).orElse(null);
    }

    /** Get or create a QuickZSet */
    private QuickZSet getOrCreate(final String key) {
        return get(key).orElseGet(() -> {
            final QuickZSet newSet = new QuickZSet();
            put(key, newSet);
            return newSet;
        });
    }
}
