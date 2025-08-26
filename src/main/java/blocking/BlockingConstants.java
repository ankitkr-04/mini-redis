package blocking;

/**
 * Constants for blocking operations in the Redis server.
 * Centralizes all magic numbers, default values, and string literals
 * used in blocking commands.
 */
public final class BlockingConstants {

    private BlockingConstants() {
        // Utility class - prevent instantiation
    }

    // Timeout and scheduling constants
    public static final long NO_TIMEOUT = -1L;
    public static final long MINIMUM_TIMEOUT_MS = 1L;
    public static final long MAXIMUM_TIMEOUT_MS = 365L * 24L * 60L * 60L * 1000L; // 1 year in ms

    // List blocking operation constants
    public static final int MINIMUM_LIST_LENGTH_FOR_AVAILABILITY = 1;
    public static final int DEFAULT_LIST_POP_COUNT = 1;

    // Stream blocking operation constants
    public static final int DEFAULT_STREAM_COUNT = -1; // -1 means no limit
    public static final int MINIMUM_STREAM_COUNT = 1;
    public static final String STREAM_ID_NEW_ENTRIES = "$"; // Special ID for new entries

    // Client management constants
    public static final int INITIAL_QUEUE_CAPACITY = 16;
    public static final String CLIENT_CLEANUP_ERROR_PREFIX = "Error during client cleanup: ";
    public static final String RESPONSE_WRITE_ERROR_PREFIX = "Error writing response to client: ";

    // Response messages
    public static final String TIMEOUT_RESPONSE_MESSAGE = "Operation timed out";
    public static final String CLIENT_DISCONNECTED_MESSAGE = "Client disconnected";

    // Collection initial capacities for performance
    public static final int INITIAL_WAITING_CLIENTS_CAPACITY = 64;
    public static final int INITIAL_CLIENT_CONTEXTS_CAPACITY = 128;

    // Validation constants
    public static final int MAXIMUM_KEYS_PER_BLOCKING_OPERATION = 1000;
    public static final int MINIMUM_KEYS_PER_BLOCKING_OPERATION = 1;
}
