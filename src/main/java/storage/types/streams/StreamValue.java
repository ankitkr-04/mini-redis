package storage.types.streams;

import java.util.concurrent.ConcurrentNavigableMap;
import storage.expiry.ExpiryPolicy;
import storage.types.StoredValue;
import storage.types.ValueType;



public record StreamValue(ConcurrentNavigableMap<String, StreamEntry> value, ExpiryPolicy expiry)
        implements StoredValue<ConcurrentNavigableMap<String, StreamEntry>> {
    @Override
    public ValueType type() {
        return ValueType.STREAM;
    }
}
