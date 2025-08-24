package config;

public final class ProtocolConstants {
    private ProtocolConstants() {
    } // Utility class
      // Data Structure Limits

    public static final int LIST_NODE_CAPACITY = 64;

    // Protocol Markers
    public static final String CRLF = "\r\n";
    public static final char SIMPLE_STRING = '+';
    public static final char ERROR = '-';
    public static final char INTEGER = ':';
    public static final char BULK_STRING = '$';
    public static final char ARRAY = '*';

    // Standard Responses
    public static final String NULL_BULK_LENGTH = "-1";
    public static final String EMPTY_ARRAY_LENGTH = "0";
    public static final String OK_RESPONSE = "OK";
    public static final String PONG_RESPONSE = "PONG";
    public static final String QUEUED_RESPONSE = "QUEUED";

    public static final String RESP_OK = SIMPLE_STRING + OK_RESPONSE + CRLF; // +OK\r\n
    public static final String RESP_PONG = SIMPLE_STRING + PONG_RESPONSE + CRLF; // +PONG\r\n
    public static final String RESP_QUEUED = SIMPLE_STRING + QUEUED_RESPONSE + CRLF; // +QUEUED\r\n
    public static final String RESP_NULL_BULK_STRING = BULK_STRING + NULL_BULK_LENGTH + CRLF; // $-1\r\n
    public static final String RESP_EMPTY_ARRAY = ARRAY + EMPTY_ARRAY_LENGTH + CRLF; // *0\r\n

    // Replication Constants
    public static final int DEFAULT_REPLICA_PORT = 6380;
    public static final int RDB_BUFFER_SIZE = 8192;
    public static final String DEFAULT_PSYNC_REPLID = "?";
    public static final String DEFAULT_PSYNC_OFFSET = "-1";
    // Empty RDB file content as a byte array (to avoid encoding issues)
    public static final byte[] EMPTY_RDB_BYTES = {
            'R', 'E', 'D', 'I', 'S', '0', '0', '1', '1', // magic + version
            (byte) 0xFF, // EOF marker
            0, 0, 0, 0, 0, 0, 0, 0 // checksum (8 bytes of zero)
    };

    public static final String EMPTY_RDB_CONTENT = "REDIS0011\u00fe\u00ff";
    public static final String FULLRESYNC_PREFIX = "FULLRESYNC";
    public static final String CONTINUE_PREFIX = "CONTINUE";

    // Replication Info Fields
    public static final String ROLE_FIELD = "role";
    public static final String MASTER_REPLID_FIELD = "master_replid";
    public static final String CONNECTED_SLAVES_FIELD = "connected_slaves";
    public static final String MASTER_REPL_OFFSET_FIELD = "master_repl_offset";
    public static final String MASTER_HOST_FIELD = "master_host";
    public static final String MASTER_PORT_FIELD = "master_port";
    public static final String HANDSHAKE_STATUS_FIELD = "handshake_status";

    public enum Role {
        MASTER("master"),
        SLAVE("slave");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum HandshakeState {
        INITIAL, PING_SENT, REPLCONF_PORT_SENT, REPLCONF_CAPA_SENT,
        PSYNC_SENT, RDB_RECEIVING, ACTIVE
    }
}