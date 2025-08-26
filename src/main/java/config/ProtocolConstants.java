package config;

/*
 * Holds protocol-related constants and utility methods for a Redis-like server.
 * This includes RESP markers, standard responses, RDB opcodes, and methods to
 * generate protocol-specific byte arrays.
 * 
 */
public final class ProtocolConstants {
    private ProtocolConstants() {
    } // Utility class

    // Protocol markers
    public static final String CRLF = "\r\n";
    public static final char SIMPLE_STRING = '+';
    public static final char ERROR = '-';
    public static final char INTEGER = ':';
    public static final char BULK_STRING = '$';
    public static final char ARRAY = '*';

    // Standard responses
    public static final String OK_RESPONSE = "OK";
    public static final String PONG_RESPONSE = "PONG";
    public static final String QUEUED_RESPONSE = "QUEUED";

    // RESP formatted responses
    public static final String RESP_OK = "+OK\r\n";
    public static final String RESP_PONG = "+PONG\r\n";
    public static final String RESP_QUEUED = "+QUEUED\r\n";
    public static final String RESP_NULL_BULK_STRING = "$-1\r\n";
    public static final String RESP_NULL_ARRAY = "*-1\r\n";
    public static final String RESP_EMPTY_ARRAY = "*0\r\n";

    // Replication constants
    public static final String FULLRESYNC_PREFIX = "FULLRESYNC";
    public static final String CONTINUE_PREFIX = "CONTINUE";

    /**
     * Generates an empty RDB file content for replication purposes.
     * Contains minimal RDB structure: header + version + EOF + checksum.
     */
    public static byte[] generateEmptyRdbBytes() {
        return new byte[] {
                'R', 'E', 'D', 'I', 'S', '0', '0', '1', '1', // magic + version (REDIS0011)
                (byte) 0xFF, // EOF marker
                0, 0, 0, 0, 0, 0, 0, 0 // checksum (8 bytes of zero)
        };
    }

    // For backward compatibility - use generateEmptyRdbBytes() in new code
    public static final byte[] EMPTY_RDB_BYTES = generateEmptyRdbBytes();

    // RDB opcodes
    public static final String RDB_FILE_HEADER = "REDIS0011";
    public static final int RDB_OPCODE_EOF = 0xFF;
    public static final int RDB_OPCODE_SELECTDB = 0xFE;
    public static final int RDB_OPCODE_EXPIRE_TIME_SEC = 0xFD;
    public static final int RDB_OPCODE_EXPIRE_TIME_MS = 0xFC;
    public static final int RDB_OPCODE_RESIZEDB = 0xFB;
    public static final int RDB_OPCODE_METADATA = 0xFA;
    public static final int RDB_KEY_INDICATOR = 0x00; // 00xxxxxx
    public static final int RDB_STRING_VALUE_INDICATOR = 0x01; // 01xxxxxx
    public static final int RDB_LIST_VALUE_INDICATOR = 0x02; // 10xxxxxx
    public static final int RDB_STREAM_VALUE_INDICATOR = 0x03;

    // Data structure limits
    /**
     * Node capacity for QuickList implementation. 64 provides optimal balance
     * between:
     * - Memory efficiency (not too many small nodes)
     * - Operation efficiency (quick traversal within nodes)
     * - Cache locality (fits well in CPU cache lines)
     * Based on Redis ziplist design principles.
     */
    public static final int LIST_NODE_CAPACITY = 64;
    public static final int RDB_BUFFER_SIZE = 8192;
}
