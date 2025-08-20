package blocking;

import java.nio.channels.SocketChannel;
import java.time.Instant;

public record BlockedClient(SocketChannel channel, Instant blockedAt, Instant timeoutAt) {
    public static BlockedClient indefinite(SocketChannel channel) {
        return new BlockedClient(channel, Instant.now(), null);
    }

    public static BlockedClient withTimeout(SocketChannel channel, double timeoutMs) {
        Instant now = Instant.now();
        Instant timeout = now.plusMillis((long) timeoutMs);
        return new BlockedClient(channel, now, timeout);
    }

    public boolean isExpired() {
        return timeoutAt != null && Instant.now().isAfter(timeoutAt);
    }

    public boolean hasTimeout() {
        return timeoutAt != null;
    }

}
