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

    public StringRepository(Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void put(String key, String value, ExpiryPolicy expiry) {
        store.put(key, StringValue.of(value, expiry));
    }

    @Override
    public Optional<String> get(String key) {
        return getValidValue(key).filter(v -> v.type() == ValueType.STRING)
                .map(v -> ((StringValue) v).value());
    }

    @Override
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public boolean exists(String key) {
        return getValidValue(key).isPresent();
    }

    @Override
    public ValueType getType(String key) {
        return getValidValue(key).map(StoredValue::type).orElse(ValueType.NONE);
    }

    public long increment(String key) {
        ensureStringKeyExists(key);

        long val = parseLongValue(get(key).orElse("0"));

        if (val == Long.MAX_VALUE) {
            throw new NumberFormatException("ERR increment or decrement would overflow");
        }

        long newVal = val + 1;
        put(key, Long.toString(newVal), ExpiryPolicy.never());
        return newVal;
    }

    private void ensureStringKeyExists(String key) {
        if (!exists(key)) {
            put(key, "0", ExpiryPolicy.never());
        }
        if (getType(key) != ValueType.STRING) {
            throw new IllegalStateException(ErrorCode.WRONG_TYPE.getMessage());
        }
    }

    private long parseLongValue(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            throw new NumberFormatException(ErrorCode.INVALID_INTEGER.getMessage());
        }
    }

    private Optional<StoredValue<?>> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            value = null;
        }
        return Optional.ofNullable(value);
    }
}
