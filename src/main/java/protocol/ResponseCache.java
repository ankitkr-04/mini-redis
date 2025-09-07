package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import config.ProtocolConstants;

/**
 * High-performance response cache for frequently used Redis responses.
 * Pre-computes and caches common responses to eliminate allocation overhead.
 * 
 * Features:
 * - Pre-computed common responses (OK, PONG, errors)
 * - Thread-safe caching of dynamic responses
 * - Zero-copy response serving
 * - Java 24 optimized implementation
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class ResponseCache {

    // Pre-computed common responses - these never change
    public static final ByteBuffer OK_RESPONSE = ByteBuffer
            .wrap(ProtocolConstants.RESP_OK.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();

    public static final ByteBuffer PONG_RESPONSE = ByteBuffer
            .wrap(ProtocolConstants.RESP_PONG.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();

    public static final ByteBuffer QUEUED_RESPONSE = ByteBuffer
            .wrap(ProtocolConstants.RESP_QUEUED.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();

    public static final ByteBuffer NULL_BULK_STRING = ByteBuffer
            .wrap(ProtocolConstants.RESP_NULL_BULK_STRING.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();

    public static final ByteBuffer NULL_ARRAY = ByteBuffer
            .wrap(ProtocolConstants.RESP_NULL_ARRAY.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();

    public static final ByteBuffer EMPTY_ARRAY = ByteBuffer
            .wrap(ProtocolConstants.RESP_EMPTY_ARRAY.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();

    // Cache for integer responses (frequently used: 0, 1, -1, etc.)
    private static final ConcurrentHashMap<Long, ByteBuffer> INTEGER_CACHE = new ConcurrentHashMap<>();

    // Cache for small bulk strings (frequently used keys/values)
    private static final ConcurrentHashMap<String, ByteBuffer> BULK_STRING_CACHE = new ConcurrentHashMap<>();

    // Cache limits to prevent memory bloat
    private static final int MAX_INTEGER_CACHE_SIZE = 1000;
    private static final int MAX_BULK_STRING_CACHE_SIZE = 1000;
    private static final int MAX_CACHED_STRING_LENGTH = 100;

    static {
        // Pre-populate common integer responses
        for (long i = -10; i <= 100; i++) {
            cacheInteger(i);
        }

        // Pre-populate common error responses
        cacheBulkString("ERR unknown command");
        cacheBulkString("ERR wrong number of arguments");
        cacheBulkString("ERR syntax error");
        cacheBulkString("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    private ResponseCache() {
        // Utility class
    }

    /**
     * Gets a cached integer response or creates and caches it.
     * 
     * @param value the integer value
     * @return cached ByteBuffer (duplicate for thread safety)
     */
    public static ByteBuffer getCachedInteger(long value) {
        ByteBuffer cached = INTEGER_CACHE.get(value);
        if (cached != null) {
            return cached.duplicate(); // Thread-safe copy
        }

        if (INTEGER_CACHE.size() < MAX_INTEGER_CACHE_SIZE) {
            return cacheInteger(value);
        }

        // Cache is full, create without caching - direct encoding
        String response = ":" + value + ProtocolConstants.CRLF;
        return ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gets a cached bulk string response or creates and caches it.
     * 
     * @param value the string value
     * @return cached ByteBuffer (duplicate for thread safety)
     */
    public static ByteBuffer getCachedBulkString(String value) {
        if (value == null) {
            return NULL_BULK_STRING.duplicate();
        }

        if (value.length() > MAX_CACHED_STRING_LENGTH) {
            // Too long to cache - direct encoding
            String response = "$" + value.length() + ProtocolConstants.CRLF + value + ProtocolConstants.CRLF;
            return ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
        }

        ByteBuffer cached = BULK_STRING_CACHE.get(value);
        if (cached != null) {
            return cached.duplicate(); // Thread-safe copy
        }

        if (BULK_STRING_CACHE.size() < MAX_BULK_STRING_CACHE_SIZE) {
            return cacheBulkString(value);
        }

        // Cache is full, create without caching - direct encoding
        String response = "$" + value.length() + ProtocolConstants.CRLF + value + ProtocolConstants.CRLF;
        return ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates and caches an integer response.
     */
    private static ByteBuffer cacheInteger(long value) {
        String response = ":" + value + ProtocolConstants.CRLF;
        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
        INTEGER_CACHE.put(value, buffer);
        return buffer.duplicate();
    }

    /**
     * Creates and caches a bulk string response.
     */
    private static ByteBuffer cacheBulkString(String value) {
        String response = "$" + value.length() + ProtocolConstants.CRLF + value + ProtocolConstants.CRLF;
        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
        BULK_STRING_CACHE.put(value, buffer);
        return buffer.duplicate();
    }

    /**
     * Gets cache statistics for monitoring.
     * 
     * @return array with [integerCacheSize, bulkStringCacheSize]
     */
    public static int[] getCacheStats() {
        return new int[] {
                INTEGER_CACHE.size(),
                BULK_STRING_CACHE.size()
        };
    }

    /**
     * Clears all caches. Useful for testing.
     */
    public static void clearCaches() {
        INTEGER_CACHE.clear();
        BULK_STRING_CACHE.clear();
    }
}
