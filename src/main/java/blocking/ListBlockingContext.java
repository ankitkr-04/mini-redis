package blocking;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import protocol.ResponseBuilder;
import storage.StorageService;

/**
 * Immutable blocking context for list operations (BLPOP, BRPOP, etc.).
 * Monitors multiple list keys and returns the first available value.
 * 
 * @param keys the list of keys to monitor for data availability
 */
public record ListBlockingContext(List<String> keys) implements BlockingContext<String> {

    /**
     * Compact constructor with validation.
     */
    public ListBlockingContext {
        Objects.requireNonNull(keys, "Keys list cannot be null");
        keys = List.copyOf(keys); // Ensure immutability

        // Validate using the interface default method
        validate();
    }

    /**
     * Creates a list blocking context for a single key.
     * 
     * @param key the single key to monitor
     * @return a new ListBlockingContext
     */
    public static ListBlockingContext forSingleKey(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return new ListBlockingContext(List.of(key));
    }

    /**
     * Creates a list blocking context for multiple keys.
     * 
     * @param keys the keys to monitor
     * @return a new ListBlockingContext
     */
    public static ListBlockingContext forKeys(String... keys) {
        Objects.requireNonNull(keys, "Keys array cannot be null");
        return new ListBlockingContext(List.of(keys));
    }

    @Override
    public boolean hasDataAvailable(String key, StorageService storage) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(storage, "Storage service cannot be null");

        return keys.contains(key) &&
                storage.getListLength(key) >= BlockingConstants.MINIMUM_LIST_LENGTH_FOR_AVAILABILITY;
    }

    @Override
    public ByteBuffer buildSuccessResponse(StorageService storage) {
        Objects.requireNonNull(storage, "Storage service cannot be null");

        // Try each key in order until we find one with data
        for (final String key : keys) {
            if (storage.getListLength(key) >= BlockingConstants.MINIMUM_LIST_LENGTH_FOR_AVAILABILITY) {
                final Optional<String> value = storage.leftPop(key);
                if (value.isPresent()) {
                    return ResponseBuilder.array(List.of(key, value.get()));
                }
            }
        }

        // This should not happen if hasDataAvailable was checked properly,
        // but provide a fallback for safety
        return ResponseBuilder.bulkString(null);
    }

    @Override
    public List<String> getMonitoredKeys() {
        return keys; // Already immutable from compact constructor
    }

    @Override
    public BlockingOperationType getOperationType() {
        return BlockingOperationType.LIST_BLOCKING;
    }

    @Override
    public void validate() {
        BlockingContext.super.validate(); // Call interface validation

        // Additional list-specific validation
        for (final String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("Keys cannot be null or empty");
            }
        }
    }

    /**
     * Gets the number of keys being monitored.
     * 
     * @return the key count
     */
    public int getKeyCount() {
        return keys.size();
    }

    /**
     * Checks if this context is monitoring the specified key.
     * 
     * @param key the key to check
     * @return true if the key is being monitored, false otherwise
     */
    public boolean isMonitoring(String key) {
        return keys.contains(key);
    }
}
