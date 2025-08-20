package storage.engines;

import java.util.Map;
import java.util.Optional;
import storage.expiry.ExpiryPolicy;
import storage.interfaces.StringStorage;
import storage.types.StoredValue;
import storage.types.StringValue;

public class StringStorageEngine implements StringStorage {
    private final Map<String, StoredValue<?>> store;

    public StringStorageEngine(Map<String, StoredValue<?>> sharedStore) {
        this.store = sharedStore;
    }

    @Override
    public void setString(String key, String value, ExpiryPolicy expiry) {
        store.put(key, StringValue.of(value, expiry));
    }

    @Override
    public Optional<String> getString(String key) {
        StoredValue<?> value = getValidValue(key);
        return switch (value) {
            case StringValue(var val, var _) -> Optional.of(val);
            case null -> Optional.empty();
            default -> Optional.empty();
        };
    }

    private StoredValue<?> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            return null;
        }
        return value;
    }
}
