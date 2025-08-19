package store;

import java.util.List;
import java.util.Optional;
import records.ExpiringValue;

public final class DataStore {
    private final KeyValueStore keyValueStore = new KeyValueStore();
    private final ListStore listStore = new ListStore();

    // Key-Value operations
    public void set(String key, ExpiringValue value) {
        keyValueStore.set(key, value);
    }

    public Optional<String> get(String key) {
        return keyValueStore.get(key);
    }

    public boolean existsKey(String key) {
        return keyValueStore.exists(key);
    }

    public boolean deleteKey(String key) {
        return keyValueStore.delete(key);
    }

    // List operations
    public int pushToList(String key, String... values) {
        return listStore.rightPush(key, values);
    }

    public int leftPushToList(String key, String... values) {
        return listStore.leftPush(key, values);
    }

    public List<String> getListRange(String key, int start, int end) {
        return listStore.getRange(key, start, end);
    }

    public int getListLength(String key) {
        return listStore.getLength(key);
    }

    public Optional<String> popFromListRight(String key) {
        return listStore.rightPop(key);
    }

    public Optional<String> popFromListLeft(String key) {
        return listStore.leftPop(key);
    }

    public List<String> popFromListLeft(String key, int elementCount) {
        return listStore.leftPop(key, elementCount);
    }

    public boolean existsList(String key) {
        return listStore.exists(key);
    }

    public boolean deleteList(String key) {
        return listStore.delete(key);
    }

    // Direct access to specialized stores (for commands that need specific functionality)
    public KeyValueStore getKeyValueStore() {
        return keyValueStore;
    }

    public ListStore getListStore() {
        return listStore;
    }

    // Global operations
    public void clearAll() {
        keyValueStore.clear();
        listStore.clear();
    }
}
