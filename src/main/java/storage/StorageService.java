package storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import storage.expiry.ExpiryPolicy;
import storage.repositories.ListRepository;
import storage.repositories.StreamRepository;
import storage.repositories.StringRepository;
import storage.types.StoredValue;
import storage.types.ValueType;
import storage.types.streams.StreamRangeEntry;

public final class StorageService {
    private final Map<String, StoredValue<?>> store = new ConcurrentHashMap<>();
    private final StringRepository stringRepo;
    private final ListRepository listRepo;
    private final StreamRepository streamRepo;

    public StorageService() {
        this.stringRepo = new StringRepository(store);
        this.listRepo = new ListRepository(store);
        this.streamRepo = new StreamRepository(store);
    }

    public Map<String, StoredValue<?>> getStore() {
        return store;
    }

    // String operations
    public void setString(String key, String value, ExpiryPolicy expiry) {
        stringRepo.put(key, value, expiry);
    }

    public Optional<String> getString(String key) {
        return stringRepo.get(key);
    }

    public long incrementString(String key) {
        return stringRepo.increment(key);
    }

    // List operations
    public int leftPush(String key, String... values) {
        return listRepo.pushLeft(key, values);
    }

    public int rightPush(String key, String... values) {
        return listRepo.pushRight(key, values);
    }

    public Optional<String> leftPop(String key) {
        return listRepo.popLeft(key);
    }

    public Optional<String> rightPop(String key) {
        return listRepo.popRight(key);
    }

    public List<String> leftPop(String key, int count) {
        return listRepo.popLeft(key, count);
    }

    public List<String> rightPop(String key, int count) {
        return listRepo.popRight(key, count);
    }

    public List<String> getListRange(String key, int start, int end) {
        return listRepo.range(key, start, end);
    }

    public int getListLength(String key) {
        return listRepo.getLength(key);
    }

    // Stream operations
    public String addStreamEntry(String key, String id, Map<String, String> fields,
            ExpiryPolicy expiry) {
        return streamRepo.addEntry(key, id, fields, expiry);
    }

    public Optional<String> getLastStreamId(String key) {
        return streamRepo.getLastId(key);
    }

    public List<StreamRangeEntry> getStreamRange(String key, String start, String end, int count) {
        return streamRepo.getRange(key, start, end, count);
    }

    public List<StreamRangeEntry> getStreamRange(String key, String start, String end) {
        return streamRepo.getRange(key, start, end, 0);
    }

    public List<StreamRangeEntry> getStreamAfter(String key, String afterId, int count) {
        return streamRepo.getAfter(key, afterId, count);
    }

    // General operations
    public boolean exists(String key) {
        return getValidValue(key) != null;
    }

    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    public ValueType getType(String key) {
        var value = getValidValue(key);
        return value != null ? value.type() : ValueType.NONE;
    }

    public List<String> getKeysByPattern(String pattern) {
        String regex = "^" + pattern
                .replace("?", ".")
                .replace("*", ".*")
                + "$";

        return store.keySet().stream()
                .filter(key -> key.matches(regex))
                .toList();
    }

    public void clear() {
        store.clear();
    }

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
