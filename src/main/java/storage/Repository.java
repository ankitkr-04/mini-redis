package storage;

import java.util.Optional;

import storage.expiry.ExpiryPolicy;
import storage.types.ValueType;

/**
 * Repository interface for storing and retrieving values with optional expiry.
 *
 * @param <T> the type of value to store
 * @author Ankit Kumar
 * @version 1.0
 */
public interface Repository<T> {

    /**
     * Stores a value with the specified key and expiry policy.
     *
     * @param key    the key to associate with the value
     * @param value  the value to store
     * @param expiry the expiry policy for the value
     */
    void put(String key, T value, ExpiryPolicy expiry);

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return an Optional containing the value if present, otherwise empty
     */
    Optional<T> get(String key);

    /**
     * Deletes the value associated with the specified key.
     *
     * @param key the key whose value is to be deleted
     * @return true if the value was deleted, false otherwise
     */
    boolean delete(String key);

    /**
     * Checks if a value exists for the specified key.
     *
     * @param key the key to check for existence
     * @return true if a value exists, false otherwise
     */
    boolean exists(String key);

    /**
     * Gets the type of value associated with the specified key.
     *
     * @param key the key whose value type is to be returned
     * @return the ValueType of the value
     */
    ValueType getType(String key);

    /**
     * Stores a value with the specified key and no expiry.
     *
     * @param key   the key to associate with the value
     * @param value the value to store
     */
    default void put(String key, T value) {
        put(key, value, ExpiryPolicy.never());
    }
}
