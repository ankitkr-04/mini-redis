package store;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Optional;
import datatype.ExpiringMetadata;
import enums.DataType;
import record.BlockedClient;

public final class DataStore {

    private final UnifiedStore unifiedStore = new UnifiedStore();
    private final BlockingClientStore blockedClientStore = new BlockingClientStore();

    // --- Key-Value operations ---
    public void set(String key, String value) {
        unifiedStore.setString(key, value);
    }

    public void set(String key, String value, ExpiringMetadata metadata) {
        unifiedStore.setString(key, value, metadata);
    }

    public Optional<String> get(String key) {
        return unifiedStore.getString(key);
    }

    public boolean existsKey(String key) {
        return unifiedStore.exists(key);
    }

    public boolean deleteKey(String key) {
        return unifiedStore.delete(key);
    }

    public DataType getKeyType(String key) {
        return unifiedStore.getType(key).orElse(DataType.NONE);
    }

    // --- List operations ---
    public int pushToList(String key, String... values) {
        return unifiedStore.rightPush(key, values);
    }

    public int leftPushToList(String key, String... values) {
        return unifiedStore.leftPush(key, values);
    }

    public List<String> getListRange(String key, int start, int end) {
        return unifiedStore.getListRange(key, start, end);
    }

    public int getListLength(String key) {
        return unifiedStore.getListLength(key);
    }

    public Optional<String> popFromListRight(String key) {
        return unifiedStore.rightPop(key);
    }

    public Optional<String> popFromListLeft(String key) {
        return unifiedStore.leftPop(key);
    }

    public List<String> popFromListLeft(String key, int elementCount) {
        return unifiedStore.leftPop(key, elementCount);
    }

    public boolean existsList(String key) {
        return unifiedStore.getType(key).map(type -> type == DataType.LIST).orElse(false);
    }

    public boolean deleteList(String key) {
        return unifiedStore.delete(key);
    }


    // --- Blocking client registry operations (unchanged) ---
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

    // --- Access to stores ---
    public UnifiedStore getUnifiedStore() {
        return unifiedStore;
    }

    public BlockingClientStore getBlockedClientStore() {
        return blockedClientStore;
    }

    // --- Global operations ---
    public void clearAll() {
        unifiedStore.clear();
        blockedClientStore.clear();
    }
}
