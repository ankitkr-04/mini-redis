package store;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import record.ExpiringValue;

/*
 * Specialized store for key-value string operations with expiration support.*Thread-safe
 * implementation using ConcurrentHashMap.
 */

public final class KeyValueStore {
    private final Map<String, ExpiringValue> store = new ConcurrentHashMap<>();

    /**
     * Stores a key-value pair with optional expiration.
     */
    public void set(String key, ExpiringValue value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }
        store.put(key, value);
    }

    /**
     * Retrieves a value by key, automatically removing expired entries.
     */
    public Optional<String> get(String key) {
        if (key == null) {
            return Optional.empty();
        }

        ExpiringValue value = store.get(key);
        if (value == null) {
            return Optional.empty();
        }

        if (value.isExpired()) {
            store.remove(key); // Lazy cleanup of expired keys
            return Optional.empty();
        }

        return Optional.of(value.value());
    }

    /**
     * Checks if a key exists and is not expired.
     */
    public boolean exists(String key) {
        return get(key).isPresent();
    }

    /**
     * Removes a key from the store.
     */
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    /**
     * Returns the number of keys in the store (including expired ones).
     */
    public int size() {
        return store.size();
    }

    /**
     * Clears all keys from the store.
     */
    public void clear() {
        store.clear();
    }
}
