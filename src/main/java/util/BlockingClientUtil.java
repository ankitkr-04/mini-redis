package util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import record.BlockedClient;
import resp.RESPFormatter;
import store.DataStore;

public final class BlockingClientUtil {
    private BlockingClientUtil() {}

    public static void wakeBlockedClients(String key, DataStore dataStore) {
        while (dataStore.hasBlockedClients(key) && dataStore.getListLength(key) > 0) {
            BlockedClient client = dataStore.popBlockedClient(key);

            if (client == null || client.isExpired())
                continue;
            Optional<String> valueOpt = dataStore.popFromListLeft(key);
            if (valueOpt.isEmpty())
                break;


            String value = valueOpt.get();
            try {
                ByteBuffer response = RESPFormatter.array(List.of(key, value));
                while (response.hasRemaining()) {
                    client.client().write(response);
                }
            } catch (IOException e) {
                // If client disconnected, ignore or log
                System.out.println(e.toString());

            }

        }
    }

}
