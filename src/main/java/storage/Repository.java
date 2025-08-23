package storage;

import java.util.Optional;
import storage.expiry.ExpiryPolicy;
import storage.types.ValueType;

public interface Repository<T> {
    void put(String key, T value, ExpiryPolicy expiry);

    Optional<T> get(String key);

    boolean delete(String key);

    boolean exists(String key);

    ValueType getType(String key);

    default void put(String key, T value) {
        put(key, value, ExpiryPolicy.never());
    }
}
