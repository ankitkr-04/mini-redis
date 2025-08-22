package storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import storage.engines.ListStorageEngine;
import storage.engines.StreamStorageEngine;
import storage.engines.StringStorageEngine;
import storage.expiry.ExpiryPolicy;
import storage.interfaces.StorageEngine;
import storage.types.StoredValue;
import storage.types.ValueType;
import storage.types.streams.StreamRangeEntry;

public final class InMemoryStorage implements StorageEngine {

    private final Map<String, StoredValue<?>> store = new ConcurrentHashMap<>();
    private final StringStorageEngine stringEngine;
    private final ListStorageEngine listEngine;
    private final StreamStorageEngine streamEngine;

    public InMemoryStorage() {
        this.stringEngine = new StringStorageEngine(store);
        this.listEngine = new ListStorageEngine(store);
        this.streamEngine = new StreamStorageEngine(store);
    }

    // String operations - delegate to string engine
    @Override
    public void setString(String key, String value, ExpiryPolicy expiry) {
        stringEngine.setString(key, value, expiry);
    }

    @Override
    public Optional<String> getString(String key) {
        return stringEngine.getString(key);
    }

    // List operations - delegate to list engine
    @Override
    public int leftPush(String key, String... values) {
        return listEngine.leftPush(key, values);
    }

    @Override
    public int rightPush(String key, String... values) {
        return listEngine.rightPush(key, values);
    }

    @Override
    public Optional<String> leftPop(String key) {
        return listEngine.leftPop(key);
    }

    @Override
    public Optional<String> rightPop(String key) {
        return listEngine.rightPop(key);
    }

    @Override
    public List<String> leftPop(String key, int count) {
        return listEngine.leftPop(key, count);
    }

    @Override
    public List<String> rightPop(String key, int count) {
        return listEngine.rightPop(key, count);
    }

    @Override
    public List<String> getListRange(String key, int start, int end) {
        return listEngine.getListRange(key, start, end);
    }

    @Override
    public int getListLength(String key) {
        return listEngine.getListLength(key);
    }

    // Stream operations - delegate to stream engine
    @Override
    public String addStreamEntry(String key, String id, Map<String, String> fields,
            ExpiryPolicy expiry) {
        return streamEngine.addStreamEntry(key, id, fields, expiry);
    }

    @Override
    public Optional<String> getLastStreamId(String key) {
        return streamEngine.getLastStreamId(key);
    }


    @Override
    public List<StreamRangeEntry> getStreamRange(String key, String start, String end, int count) {
        return streamEngine.getStreamRange(key, start, end, count);

    }

    @Override
    public List<StreamRangeEntry> getStreamRange(String key, String start, String end) {
        return streamEngine.getStreamRange(key, start, end);
    }

    @Override
    public List<StreamRangeEntry> getStreamAfter(String key, String afterId, int count) {
        return streamEngine.getStreamAfter(key, afterId, count);
    }

    // General operations
    @Override
    public boolean exists(String key) {
        return getValidValue(key) != null;
    }

    @Override
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public ValueType getType(String key) {
        var value = getValidValue(key);
        return value != null ? value.type() : ValueType.NONE;
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public void cleanup() {
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
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
