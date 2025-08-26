package storage.types.streams;

import java.util.concurrent.ConcurrentNavigableMap;

import storage.expiry.ExpiryPolicy;
import storage.types.StoredValue;
import storage.types.ValueType;

/**
 * Represents a stream value in the storage system.
 * Holds a navigable map of stream entries and an expiry policy.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public record StreamValue(
        ConcurrentNavigableMap<String, StreamEntry> streamEntries,
        ExpiryPolicy expiryPolicy) implements StoredValue<ConcurrentNavigableMap<String, StreamEntry>> {

    /**
     * The value type for this stored value.
     */

    public static final ValueType STREAM_VALUE_TYPE = ValueType.STREAM;

    @Override
    public ValueType type() {
        return STREAM_VALUE_TYPE;
    }

    @Override
    public ConcurrentNavigableMap<String, StreamEntry> value() {
        return streamEntries;
    }

    @Override
    public ExpiryPolicy expiry() {
        return expiryPolicy;
    }
}