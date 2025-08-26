package storage.types;

import collections.QuickZSet;
import storage.expiry.ExpiryPolicy;

/**
 * Represents a stored sorted set (ZSet) value with an associated expiry policy.
 * Used for data persistence in the storage layer.
 */
public final class ZSetValue implements StoredValue<QuickZSet> {

    private static final ValueType VALUE_TYPE = ValueType.ZSET;

    private final QuickZSet zset;
    private final ExpiryPolicy expiryPolicy;

    /**
     * Constructs a ZSetValue with the specified sorted set and expiry policy.
     *
     * @param zset         the sorted set value to store
     * @param expiryPolicy the expiry policy for this value
     */
    public ZSetValue(QuickZSet zset, ExpiryPolicy expiryPolicy) {
        this.zset = zset;
        this.expiryPolicy = expiryPolicy;
    }

    /**
     * Returns the stored sorted set.
     *
     * @return the QuickZSet instance
     */
    @Override
    public QuickZSet value() {
        return zset;
    }

    /**
     * Returns the expiry policy associated with this value.
     *
     * @return the ExpiryPolicy instance
     */
    @Override
    public ExpiryPolicy expiry() {
        return expiryPolicy;
    }

    /**
     * Returns the type of value stored.
     *
     * @return ValueType.ZSET
     */
    @Override
    public ValueType type() {
        return VALUE_TYPE;
    }
}
