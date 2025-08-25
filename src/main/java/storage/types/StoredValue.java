package storage.types;

import storage.expiry.ExpiryPolicy;
import storage.types.streams.StreamValue;

public sealed interface StoredValue<T> permits StringValue, ListValue, StreamValue, ZSetValue {

    T value();

    ExpiryPolicy expiry();

    ValueType type();

    default boolean isExpired() {
        return expiry().isExpired();
    }
}
