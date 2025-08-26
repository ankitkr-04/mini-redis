package storage.types;

import storage.expiry.ExpiryPolicy;
import storage.types.streams.StreamValue;

/**
 * Represents a value stored in the storage system with an associated expiry
 * policy and type.
 * 
 * @param <T> the type of the stored value
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public sealed interface StoredValue<T> permits StringValue, ListValue, StreamValue, ZSetValue {

    /**
     * Returns the actual stored value.
     *
     * @return the stored value
     */
    T value();

    /**
     * Returns the expiry policy associated with this value.
     *
     * @return the expiry policy
     */
    ExpiryPolicy expiry();

    /**
     * Returns the type of the stored value.
     *
     * @return the value type
     */
    ValueType type();

    /**
     * Checks if the value has expired based on its expiry policy.
     *
     * @return {@code true} if expired, {@code false} otherwise
     */
    default boolean isExpired() {
        return expiry().isExpired();
    }
}
