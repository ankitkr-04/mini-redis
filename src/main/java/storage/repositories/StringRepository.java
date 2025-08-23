package storage.repositories;

import java.util.Map;
import java.util.Optional;
import errors.ErrorCode;
import storage.Repository;
import storage.expiry.ExpiryPolicy;
import storage.types.StoredValue;
import storage.types.StringValue;
import storage.types.ValueType;

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
        return getValidValue(key)
                .filter(StringValue.class::isInstance)
                .map(StringValue.class::cast)
                .map(StringValue::value);
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
        return getValidValue(key)
                .map(StoredValue::type)
                .orElse(ValueType.NONE);
    }

    // change signature to return long
    public long increment(String key) {
        if (!exists(key)) {
            put(key, "0", ExpiryPolicy.never());
        }

        // ensure it's a string value
        if (getType(key) != ValueType.STRING) {
            throw new IllegalStateException(ErrorCode.WRONG_TYPE.getMessage());
        }

        String currentValue = get(key).orElse("0");
        try {
            long val = Long.parseLong(currentValue.trim());
            if (val == Long.MAX_VALUE) {
                // match Redis behaviour: overflow error
                throw new NumberFormatException("ERR increment or decrement would overflow");
            }
            val = val + 1L;
            put(key, Long.toString(val), ExpiryPolicy.never());
            return val;
        } catch (NumberFormatException e) {
            // normalize error message so upper layers can return it
            throw new NumberFormatException(ErrorCode.INVALID_INTEGER.getMessage());
        }
    }

    private Optional<StoredValue<?>> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(value);
    }
}
