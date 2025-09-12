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
import utils.GeoUtils;

public final class GeoRepository implements Repository<QuickZSet> {

    private final Map<String, StoredValue<?>> store;

    public GeoRepository(final Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void put(final String key, final QuickZSet value, final ExpiryPolicy expiry) {
        store.put(key, new ZSetValue(value, expiry));
    }

    @Override
    public Optional<QuickZSet> get(final String key) {
        return Optional.ofNullable(store.get(key))
                .filter(v -> v.type() == ValueType.ZSET)
                .map(v -> ((ZSetValue) v).value());
    }

    @Override
    public boolean delete(final String key) {
        return store.remove(key) != null;
    }

    @Override
    public boolean exists(final String key) {
        return get(key).isPresent();
    }

    @Override
    public ValueType getType(final String key) {
        return get(key).isPresent() ? ValueType.ZSET : ValueType.NONE;
    }

    // === GEO operations ===

    public boolean geoAdd(final String key, final double longitude, final double latitude, final String member,
            final ExpiryPolicy expiry) {
        final QuickZSet zset = getOrCreate(key, expiry);
        final double score = GeoUtils.encodeGeoHash(longitude, latitude);
        return zset.add(member, score);
    }

    public Double geoDist(final String key, final String member1, final String member2, final GeoUtils.GEO_UNIT unit) {
        return get(key).map(zset -> {
            final Double s1 = zset.getScore(member1);
            final Double s2 = zset.getScore(member2);
            if (s1 == null || s2 == null)
                return null;

            final double[] c1 = GeoUtils.decodeGeoHash(s1);
            final double[] c2 = GeoUtils.decodeGeoHash(s2);

            final double distanceMeters = GeoUtils.haversine(c1[0], c1[1], c2[0], c2[1]);
            return GeoUtils.convertDistance(distanceMeters, unit); // convert to requested unit
        }).orElse(null);
    }

    public Map<String, double[]> geoPos(final String key, final List<String> members) {
        return get(key).map(zset -> {
            final Map<String, double[]> map = new java.util.HashMap<>();
            for (final String m : members) {
                final Double score = zset.getScore(m);
                if (score != null) {
                    map.put(m, GeoUtils.decodeGeoHash(score));
                }
            }
            return map;
        }).orElse(Map.of());
    }

    public List<String> geoSearch(final String key, final double longitude, final double latitude,
            final double radiusMeters) {
        return get(key).map(zset -> {
            final List<String> result = new java.util.ArrayList<>();
            for (final ZSetEntry entry : zset.range(0, -1)) {
                final double[] coords = GeoUtils.decodeGeoHash(entry.score());
                final double dist = GeoUtils.haversine(longitude, latitude, coords[0], coords[1]);
                if (dist <= radiusMeters) {
                    result.add(entry.member());
                }
            }
            return result;
        }).orElse(List.of());
    }

    private QuickZSet getOrCreate(final String key, final ExpiryPolicy expiry) {
        return get(key).orElseGet(() -> {
            final QuickZSet newSet = new QuickZSet();
            put(key, newSet, expiry);
            return newSet;
        });
    }
}
