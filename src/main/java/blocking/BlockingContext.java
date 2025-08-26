package blocking;

import java.nio.ByteBuffer;
import java.util.List;

import storage.StorageService;

/**
 * Generic interface for blocking operation contexts.
 * Defines the contract for checking data availability and building responses
 * for different types of blocking operations (lists, streams, etc.).
 * 
 * @param <T> the type of keys this context operates on
 */
public interface BlockingContext<T> {

    /**
     * Checks if data is available for the specified key.
     * 
     * @param key     the key to check for data availability
     * @param storage the storage service to query
     * @return true if data is available, false otherwise
     */
    boolean hasDataAvailable(T key, StorageService storage);

    /**
     * Builds a success response when data becomes available.
     * 
     * @param storage the storage service to retrieve data from
     * @return a ByteBuffer containing the formatted response
     */
    ByteBuffer buildSuccessResponse(StorageService storage);

    /**
     * Gets the list of keys this context is monitoring.
     * 
     * @return an immutable list of keys
     */
    List<T> getMonitoredKeys();

    /**
     * Validates the context configuration.
     * Implementations should validate their specific parameters.
     * 
     * @throws IllegalArgumentException if the context is invalid
     */
    default void validate() {
        final List<T> keys = getMonitoredKeys();
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Context must monitor at least one key");
        }
        if (keys.size() > BlockingConstants.MAXIMUM_KEYS_PER_BLOCKING_OPERATION) {
            throw new IllegalArgumentException("Too many keys: " + keys.size());
        }
    }

    /**
     * Gets the type of blocking operation this context represents.
     * Useful for metrics, logging, and debugging.
     * 
     * @return the operation type
     */
    BlockingOperationType getOperationType();

    /**
     * Enumeration of supported blocking operation types.
     */
    enum BlockingOperationType {
        LIST_BLOCKING("LIST"),
        STREAM_BLOCKING("STREAM"),
        SORTED_SET_BLOCKING("ZSET"),
        CUSTOM_BLOCKING("CUSTOM");

        private final String displayName;

        BlockingOperationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
