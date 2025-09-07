package storage.repositories;

import java.util.Map;
import java.util.Optional;

import errors.ErrorCode;
import storage.Repository;
import storage.expiry.ExpiryPolicy;
import storage.types.StoredValue;
import storage.types.StringValue;
import storage.types.ValueType;

/**
 * StringRepository implementation.
 *
 * <p>
 * Storage layer implementation for data persistence.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */

public final class StringRepository implements Repository<String> {

    private final Map<String, StoredValue<?>> store;

    public StringRepository(final Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void put(final String key, final String value, final ExpiryPolicy expiry) {
        store.put(key, StringValue.of(value, expiry));
    }

    @Override
    public Optional<String> get(final String key) {
        return getValidValue(key).filter(v -> v.type() == ValueType.STRING)
                .map(v -> ((StringValue) v).value());
    }

    @Override
    public boolean delete(final String key) {
        return store.remove(key) != null;
    }

    @Override
    public boolean exists(final String key) {
        return getValidValue(key).isPresent();
    }

    @Override
    public ValueType getType(final String key) {
        return getValidValue(key).map(StoredValue::type).orElse(ValueType.NONE);
    }

    public long increment(final String key) {
        // Optimized single-pass increment
        final Optional<StoredValue<?>> currentValue = getValidValue(key);

        if (currentValue.isEmpty()) {
            // Key doesn't exist, start from 0
            put(key, "1", ExpiryPolicy.never());
            return 1L;
        }

        final StoredValue<?> storedValue = currentValue.get();
        if (!(storedValue instanceof StringValue)) {
            throw new IllegalStateException(ErrorCode.WRONG_TYPE.getMessage());
        }

        final String currentStr = ((StringValue) storedValue).value();
        final long currentLong = parseLongValue(currentStr);

        if (currentLong == Long.MAX_VALUE) {
            throw new NumberFormatException("increment or decrement would overflow");
        }

        final long newVal = currentLong + 1;
        put(key, Long.toString(newVal), ExpiryPolicy.never());
        return newVal;
    }

    public long decrement(final String key) {
        // Optimized single-pass decrement
        final Optional<StoredValue<?>> currentValue = getValidValue(key);

        if (currentValue.isEmpty()) {
            // Key doesn't exist, start from 0
            put(key, "-1", ExpiryPolicy.never());
            return -1L;
        }

        final StoredValue<?> storedValue = currentValue.get();
        if (!(storedValue instanceof StringValue)) {
            throw new IllegalStateException(ErrorCode.WRONG_TYPE.getMessage());
        }

        final String currentStr = ((StringValue) storedValue).value();
        final long currentLong = parseLongValue(currentStr);

        if (currentLong == Long.MIN_VALUE) {
            throw new NumberFormatException("increment or decrement would overflow");
        }

        final long newVal = currentLong - 1;
        put(key, Long.toString(newVal), ExpiryPolicy.never());
        return newVal;
    }

    private void ensureStringKeyExists(final String key) {
        if (!exists(key)) {
            put(key, "0", ExpiryPolicy.never());
        }
        if (getType(key) != ValueType.STRING) {
            throw new IllegalStateException(ErrorCode.WRONG_TYPE.getMessage());
        }
    }

    private long parseLongValue(final String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (final Exception e) {
            throw new NumberFormatException(ErrorCode.INVALID_INTEGER.getMessage());
        }
    }

    private Optional<StoredValue<?>> getValidValue(final String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            value = null;
        }
        return Optional.ofNullable(value);
    }
}
