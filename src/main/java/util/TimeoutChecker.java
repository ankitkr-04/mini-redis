package util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Queue;
import record.BlockedClient;
import resp.RESPFormatter;
import store.DataStore;

public class TimeoutChecker {
    private static final int INTERVAL_IN_MS = 100;

    private final DataStore dataStore;

    public TimeoutChecker(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void start() {
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    Thread.sleep(INTERVAL_IN_MS); // Check every 100ms
                    checkExpiredClients();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void checkExpiredClients() {
        for (String key : new ArrayList<>(dataStore.getBlockedClientStore().getAll().keySet())) {
            Queue<BlockedClient> expired =
                    dataStore.getBlockedClientStore().removeExpiredClients(key);
            for (BlockedClient bc : expired) {
                try {
                    ByteBuffer resp = RESPFormatter.nullBulkString();
                    while (resp.hasRemaining()) {
                        bc.client().write(resp);
                    }
                } catch (IOException e) {
                    // Ignore or log
                }
            }
        }
    }
}
