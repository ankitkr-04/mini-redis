package store;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Optional;
import record.BlockedClient;
import record.ExpiringValue;

public final class DataStore {

    private final KeyValueStore keyValueStore = new KeyValueStore();
    private final ListStore listStore = new ListStore();
    private final BlockingClientStore blockedClientStore = new BlockingClientStore();

    // --- Key-Value operations ---
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

    // --- List operations ---
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

    // --- Blocking client registry operations ---
    public void addBlockedClient(String key, SocketChannel client, double timeoutEndMillis) {
        blockedClientStore.addClient(key, client, timeoutEndMillis);
    }

    public void addBlockedClient(String key, SocketChannel client) {
        blockedClientStore.addClient(key, client);
    }

    public void removeBlockedClient(String key, SocketChannel client) {
        blockedClientStore.removeClient(key, client);
    }

    public BlockedClient popBlockedClient(String key) {
        return blockedClientStore.pollClient(key);
    }

    public boolean hasBlockedClients(String key) {
        return blockedClientStore.hasClients(key);
    }

    // --- Access specialized stores ---
    public KeyValueStore getKeyValueStore() {
        return keyValueStore;
    }

    public ListStore getListStore() {
        return listStore;
    }

    public BlockingClientStore getBlockedClientStore() {
        return blockedClientStore;
    }

    // --- Global operations ---
    public void clearAll() {
        keyValueStore.clear();
        listStore.clear();
        blockedClientStore.clear();
    }
}
