package blocking;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import protocol.ResponseBuilder;
import storage.StorageService;

/**
 * Immutable blocking context for stream operations (XREAD BLOCK, etc.).
 * Monitors multiple stream keys starting from specified IDs with optional count
 * limits.
 * 
 * @param keys  the list of stream keys to monitor
 * @param ids   the list of stream IDs to start reading from (corresponding to
 *              keys)
 * @param count optional maximum number of entries to return per stream
 */
public record StreamBlockingContext(
        List<String> keys,
        List<String> ids,
        Optional<Integer> count) implements BlockingContext<String> {

    /**
     * Compact constructor with validation.
     */
    public StreamBlockingContext {
        Objects.requireNonNull(keys, "Keys list cannot be null");
        Objects.requireNonNull(ids, "IDs list cannot be null");
        Objects.requireNonNull(count, "Count optional cannot be null");

        keys = List.copyOf(keys); // Ensure immutability
        ids = List.copyOf(ids); // Ensure immutability

        // Validate inputs
        validate();
        validateStreamSpecific();
    }

    /**
     * Creates a stream blocking context for a single stream.
     * 
     * @param key   the stream key to monitor
     * @param id    the stream ID to start from
     * @param count optional count limit
     * @return a new StreamBlockingContext
     */
    public static StreamBlockingContext forSingleStream(String key, String id, Optional<Integer> count) {
        return new StreamBlockingContext(List.of(key), List.of(id), count);
    }

    /**
     * Creates a stream blocking context for a single stream with default count.
     * 
     * @param key the stream key to monitor
     * @param id  the stream ID to start from
     * @return a new StreamBlockingContext with no count limit
     */
    public static StreamBlockingContext forSingleStream(String key, String id) {
        return forSingleStream(key, id, Optional.empty());
    }

    /**
     * Creates a stream blocking context for multiple streams.
     * 
     * @param keys  the stream keys to monitor
     * @param ids   the stream IDs to start from
     * @param count optional count limit
     * @return a new StreamBlockingContext
     */
    public static StreamBlockingContext forMultipleStreams(List<String> keys, List<String> ids,
            Optional<Integer> count) {
        return new StreamBlockingContext(keys, ids, count);
    }

    @Override
    public boolean hasDataAvailable(String key, StorageService storage) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(storage, "Storage service cannot be null");

        final int keyIndex = keys.indexOf(key);
        if (keyIndex == -1) {
            return false;
        }

        final String afterId = ids.get(keyIndex);
        final var entries = storage.getStreamAfter(key, afterId, BlockingConstants.MINIMUM_STREAM_COUNT);
        return !entries.isEmpty();
    }

    @Override
    public ByteBuffer buildSuccessResponse(StorageService storage) {
        Objects.requireNonNull(storage, "Storage service cannot be null");

        final List<ByteBuffer> responses = new ArrayList<>(keys.size());

        for (int i = 0; i < keys.size(); i++) {
            final String key = keys.get(i);
            final String afterId = ids.get(i);
            final int limit = count.orElse(BlockingConstants.DEFAULT_STREAM_COUNT);

            final var entries = storage.getStreamAfter(key, afterId, limit);
            if (!entries.isEmpty()) {
                final ByteBuffer streamResponse = ResponseBuilder.arrayOfBuffers(List.of(
                        ResponseBuilder.bulkString(key),
                        ResponseBuilder.streamEntries(entries, e -> e.id(), e -> e.fieldList())));
                responses.add(streamResponse);
            }
        }

        if (responses.isEmpty()) {
            // This should not happen if hasDataAvailable was checked properly,
            // but provide a fallback for safety
            return ResponseBuilder.bulkString(null);
        }

        return ResponseBuilder.arrayOfBuffers(responses);
    }

    @Override
    public List<String> getMonitoredKeys() {
        return keys; // Already immutable from compact constructor
    }

    @Override
    public BlockingOperationType getOperationType() {
        return BlockingOperationType.STREAM_BLOCKING;
    }

    @Override
    public void validate() {
        BlockingContext.super.validate(); // Call interface validation

        // Additional validation for keys
        for (final String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("Stream keys cannot be null or empty");
            }
        }
    }

    /**
     * Validates stream-specific constraints.
     */
    private void validateStreamSpecific() {
        if (keys.size() != ids.size()) {
            throw new IllegalArgumentException(
                    String.format("Keys and IDs lists must have the same size: keys=%d, ids=%d",
                            keys.size(), ids.size()));
        }

        // Validate stream IDs
        for (int i = 0; i < ids.size(); i++) {
            final String id = ids.get(i);
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Stream IDs cannot be null or empty at index " + i);
            }
        }

        // Validate count if present
        count.ifPresent(c -> {
            if (c < BlockingConstants.MINIMUM_STREAM_COUNT) {
                throw new IllegalArgumentException("Stream count must be positive: " + c);
            }
        });
    }

    /**
     * Gets the stream ID for the specified key.
     * 
     * @param key the stream key
     * @return the corresponding stream ID, or null if key not found
     */
    public String getStreamIdFor(String key) {
        final int index = keys.indexOf(key);
        return index >= 0 ? ids.get(index) : null;
    }

    /**
     * Gets the effective count limit.
     * 
     * @return the count limit, or -1 if no limit is set
     */
    public int getEffectiveCount() {
        return count.orElse(BlockingConstants.DEFAULT_STREAM_COUNT);
    }

    /**
     * Checks if this context has a count limit.
     * 
     * @return true if count limit is set, false otherwise
     */
    public boolean hasCountLimit() {
        return count.isPresent();
    }

    /**
     * Gets the number of streams being monitored.
     * 
     * @return the stream count
     */
    public int getStreamCount() {
        return keys.size();
    }
}
