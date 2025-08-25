package storage.types;

import java.util.NavigableMap;

import storage.expiry.ExpiryPolicy;

public final class ZSetValue implements StoredValue<NavigableMap<Double, String>> {
    private final NavigableMap<Double, String> value;
    private final ExpiryPolicy expiry;

    public ZSetValue(NavigableMap<Double, String> value, ExpiryPolicy expiry) {
        this.value = value;
        this.expiry = expiry;
    }

    @Override
    public NavigableMap<Double, String> value() {
        return value;
    }

    @Override
    public ExpiryPolicy expiry() {
        return expiry;
    }

    @Override
    public ValueType type() {
        return ValueType.ZSET;
    }
}
