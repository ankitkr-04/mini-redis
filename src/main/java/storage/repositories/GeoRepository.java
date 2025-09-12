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

/**
 * Repository implementation for geospatial data operations.
 * 
 * This class provides Redis-compatible geospatial functionality by storing
 * coordinates as encoded scores in sorted sets (ZSet). Each geographic location
 * is encoded using a geohash algorithm and stored with its member name.
 * 
 * Operations include adding locations, calculating distances, retrieving positions,
 * and performing radius-based searches.
 */
public final class GeoRepository implements Repository<QuickZSet> {

    private final Map<String, StoredValue<?>> store;

    public GeoRepository(final Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    // ==== Core Repository Operations ====

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

    // ==== Geospatial Operations ====

    /**
     * Add a geospatial location to the set.
     * Returns true if the member was newly added, false if updated.
     */
    public boolean geoAdd(final String key, final double longitude, final double latitude, 
                         final String member, final ExpiryPolicy expiry) {
        if (!GeoUtils.isValidCoordinates(longitude, latitude)) {
            throw new IllegalArgumentException("Invalid coordinates: " + longitude + ", " + latitude);
        }

        final QuickZSet zset = getOrCreateZSet(key, expiry);
        final double geohashScore = GeoUtils.encodeGeohash(longitude, latitude);
        
        return zset.add(member, geohashScore);
    }

    /**
     * Calculate distance between two members in the geospatial set.
     * Returns null if either member doesn't exist.
     */
    public Double geoDistance(final String key, final String member1, final String member2, 
                            final GeoUtils.GeoUnit unit) {
        return get(key).map(zset -> {
            final Double score1 = zset.getScore(member1);
            final Double score2 = zset.getScore(member2);
            
            if (score1 == null || score2 == null) {
                return null;
            }

            final double[] coordinates1 = GeoUtils.decodeGeohash(score1);
            final double[] coordinates2 = GeoUtils.decodeGeohash(score2);

            final double distanceMeters = GeoUtils.calculateDistance(
                coordinates1[0], coordinates1[1], 
                coordinates2[0], coordinates2[1]);
                
            return unit.fromMeters(distanceMeters);
        }).orElse(null);
    }

    /**
     * Get positions (longitude, latitude) for the specified members.
     * Returns a map with coordinates for existing members only.
     */
    public Map<String, double[]> geoPosition(final String key, final List<String> members) {
        return get(key).map(zset -> {
            final Map<String, double[]> result = new java.util.HashMap<>();
            
            for (final String member : members) {
                final Double score = zset.getScore(member);
                if (score != null) {
                    result.put(member, GeoUtils.decodeGeohash(score));
                }
            }
            
            return result;
        }).orElse(Map.of());
    }

    /**
     * Search for members within a specified radius from given coordinates.
     * Returns list of member names within the search radius.
     */
    public List<String> geoSearch(final String key, final double longitude, final double latitude,
                                 final double radiusMeters) {
        if (!GeoUtils.isValidCoordinates(longitude, latitude)) {
            throw new IllegalArgumentException("Invalid search coordinates: " + longitude + ", " + latitude);
        }

        return get(key).map(zset -> {
            final List<String> result = new java.util.ArrayList<>();
            
            // Iterate through all members and check distance
            for (final ZSetEntry entry : zset.range(0, -1)) {
                final double[] memberCoordinates = GeoUtils.decodeGeohash(entry.score());
                final double distance = GeoUtils.calculateDistance(
                    longitude, latitude, 
                    memberCoordinates[0], memberCoordinates[1]);
                    
                if (distance <= radiusMeters) {
                    result.add(entry.member());
                }
            }
            
            return result;
        }).orElse(List.of());
    }

    // ==== Private Helper Methods ====

    /**
     * Get existing ZSet or create a new one if it doesn't exist.
     */
    private QuickZSet getOrCreateZSet(final String key, final ExpiryPolicy expiry) {
        return get(key).orElseGet(() -> {
            final QuickZSet newSet = new QuickZSet();
            put(key, newSet, expiry);
            return newSet;
        });
    }
}
