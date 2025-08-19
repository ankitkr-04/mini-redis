package record;

import java.nio.channels.SocketChannel;

public record BlockedClient(SocketChannel client, long timeoutEndMillis) {
    private static final long NO_EXPIRY = 0L;

    public boolean isExpired() {
        return timeoutEndMillis != 0 && System.currentTimeMillis() >= timeoutEndMillis;
    }

    public static BlockedClient withExpiry(SocketChannel client, long timeoutEndMillis) {
        return new BlockedClient(client, timeoutEndMillis);
    }

    public static BlockedClient withOutExpiry(SocketChannel client) {
        return new BlockedClient(client, NO_EXPIRY);
    }
}
