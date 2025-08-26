package blocking;

import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable record representing a blocked client with timeout information.
 * Encapsulates client channel, blocking timestamp, and optional timeout.
 * 
 * @param channel   the client's socket channel
 * @param blockedAt when the client was blocked
 * @param timeoutAt when the client should timeout (null for indefinite
 *                  blocking)
 */
public record BlockedClient(
        SocketChannel channel,
        Instant blockedAt,
        Instant timeoutAt) {

    /**
     * Compact constructor with validation using Java 24 features.
     */
    public BlockedClient {
        Objects.requireNonNull(channel, "Channel cannot be null");
        Objects.requireNonNull(blockedAt, "Blocked timestamp cannot be null");

        // Validate timeout is in the future if specified
        if (timeoutAt != null && !timeoutAt.isAfter(blockedAt)) {
            throw new IllegalArgumentException("Timeout must be after blocked timestamp");
        }
    }

    /**
     * Creates a blocked client with indefinite blocking (no timeout).
     * 
     * @param channel the client's socket channel
     * @return a BlockedClient with no timeout
     */
    public static BlockedClient indefinite(SocketChannel channel) {
        return new BlockedClient(channel, Instant.now(), null);
    }

    /**
     * Creates a blocked client with a specific timeout.
     * 
     * @param channel   the client's socket channel
     * @param timeoutMs timeout in milliseconds
     * @return a BlockedClient with the specified timeout
     * @throws IllegalArgumentException if timeoutMs is negative or exceeds maximum
     */
    public static BlockedClient withTimeout(SocketChannel channel, long timeoutMs) {
        if (timeoutMs < BlockingConstants.MINIMUM_TIMEOUT_MS) {
            throw new IllegalArgumentException("Timeout must be positive: " + timeoutMs);
        }
        if (timeoutMs > BlockingConstants.MAXIMUM_TIMEOUT_MS) {
            throw new IllegalArgumentException("Timeout exceeds maximum: " + timeoutMs);
        }

        final Instant now = Instant.now();
        final Instant timeout = now.plusMillis(timeoutMs);
        return new BlockedClient(channel, now, timeout);
    }

    /**
     * Checks if this client's timeout has expired.
     * 
     * @return true if the client has timed out, false otherwise
     */
    public boolean isExpired() {
        return hasTimeout() && Instant.now().isAfter(timeoutAt);
    }

    /**
     * Checks if this client has a timeout configured.
     * 
     * @return true if timeout is set, false for indefinite blocking
     */
    public boolean hasTimeout() {
        return timeoutAt != null;
    }

    /**
     * Checks if the client's channel is still open and connected.
     * 
     * @return true if the channel is open, false otherwise
     */
    public boolean isChannelOpen() {
        return channel.isOpen();
    }

    /**
     * Gets the duration this client has been blocked in milliseconds.
     * 
     * @return blocking duration in milliseconds
     */
    public long getBlockingDurationMs() {
        return java.time.Duration.between(blockedAt, Instant.now()).toMillis();
    }

    /**
     * Gets the remaining timeout in milliseconds.
     * 
     * @return remaining timeout in milliseconds, or -1 if no timeout
     */
    public long getRemainingTimeoutMs() {
        if (!hasTimeout()) {
            return BlockingConstants.NO_TIMEOUT;
        }
        return java.time.Duration.between(Instant.now(), timeoutAt).toMillis();
    }
}
