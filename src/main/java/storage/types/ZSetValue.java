package storage.types;

import collections.QuickZSet;
import storage.expiry.ExpiryPolicy;

public final class ZSetValue implements StoredValue<QuickZSet> {
    private final QuickZSet value;
    private final ExpiryPolicy expiry;

    public ZSetValue(QuickZSet value, ExpiryPolicy expiry) {
        this.value = value;
        this.expiry = expiry;
    }

    @Override
    public QuickZSet value() {
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
