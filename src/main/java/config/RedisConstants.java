package config;

public final class RedisConstants {
    private RedisConstants() {} // Utility class

    // Protocol Constants
    public static final String CRLF = "\r\n";
    public static final String NULL_BULK_STRING = "$-1" + CRLF;
    public static final String EMPTY_ARRAY = "*0" + CRLF;
    public static final String OK_RESPONSE = "+OK" + CRLF;
    public static final String PONG_RESPONSE = "+PONG" + CRLF;
    public static final String ERROR_PREFIX = "-ERR ";
    public static final String QUEUED_RESPONSE = "+QUEUED" + CRLF;

    // Data Structure Defaults
    public static final int LIST_NODE_CAPACITY = 64;
    public static final int DEFAULT_STREAM_RANGE_LIMIT = 10;

    // Response Types
    public static final char SIMPLE_STRING = '+';
    public static final char ERROR = '-';
    public static final char INTEGER = ':';
    public static final char BULK_STRING = '$';
    public static final char ARRAY = '*';
}
