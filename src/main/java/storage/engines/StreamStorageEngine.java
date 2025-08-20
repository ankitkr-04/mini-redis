package storage.engines;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import common.ValidationUtil;
import storage.expiry.ExpiryPolicy;
import storage.interfaces.StreamStorage;
import storage.types.StoredValue;
import storage.types.streams.StreamEntry;
import storage.types.streams.StreamValue;

public class StreamStorageEngine implements StreamStorage {
    private final Map<String, StoredValue<?>> store;

    public StreamStorageEngine(Map<String, StoredValue<?>> sharedStore) {
        this.store = sharedStore;
    }

    @Override
    public String addStreamEntry(String key, String id, Map<String, String> fields,
            ExpiryPolicy expiry) {
        // Create or get existing StreamValue
        StreamValue streamValue = (StreamValue) store.compute(key, (k, existing) -> {
            if (existing instanceof StreamValue sv && !sv.isExpired()) {
                return sv;
            }
            return new StreamValue(new ConcurrentSkipListMap<>(), expiry);
        });

        String entryId = generateOrValidateId(key, id, streamValue);

        // Create entry and put into stream
        StreamEntry entry = new StreamEntry(entryId, fields);
        streamValue.value().put(entryId, entry);

        return entryId;
    }

    @Override
    public Optional<String> getLastStreamId(String key) {
        StoredValue<?> value = getValidValue(key);
        if (value instanceof StreamValue sv && !sv.value().isEmpty()) {
            return Optional.of(sv.value().lastKey());
        }
        return Optional.empty();
    }

    private String generateOrValidateId(String key, String id, StreamValue streamValue) {
        return switch (id) {
            case "*" -> generateAutoId(key);
            case String s when s.endsWith("-*") -> generateTimestampId(key, s);
            default -> validateExplicitId(key, id, streamValue);
        };
    }

    private String generateAutoId(String key) {
        long timestamp = System.currentTimeMillis();
        long seq = 0L;

        Optional<String> lastIdOpt = getLastStreamId(key);
        if (lastIdOpt.isPresent()) {
            String lastId = lastIdOpt.get();
            String[] parts = lastId.split("-");
            long lastMs = Long.parseLong(parts[0]);
            long lastSeq = Long.parseLong(parts[1]);

            if (timestamp <= lastMs) {
                timestamp = lastMs;
                seq = lastSeq + 1;
            }
        }

        return timestamp + "-" + seq;
    }

    private String generateTimestampId(String key, String id) {
        String[] parts = id.split("-");
        if (parts.length != 2 || !parts[1].equals("*")) {
            throw new IllegalArgumentException("Invalid stream ID format");
        }

        try {
            long timestamp = Long.parseLong(parts[0]);
            long seq = (timestamp == 0) ? 1 : 0; // Default seq: 1 for 0-*, 0 otherwise

            // Find last sequence number for this timestamp
            Optional<String> lastIdOpt = getLastStreamId(key);
            if (lastIdOpt.isPresent()) {
                String lastId = lastIdOpt.get();
                String[] lastParts = lastId.split("-");
                long lastMs = Long.parseLong(lastParts[0]);
                long lastSeq = Long.parseLong(lastParts[1]);

                if (lastMs == timestamp) {
                    seq = lastSeq + 1; // Increment sequence for same timestamp
                } else if (timestamp < lastMs) {
                    throw new IllegalArgumentException(
                            "The ID specified in XADD is equal or smaller than the target stream top item");
                }
            }

            return timestamp + "-" + seq;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stream ID format");
        }
    }

    private String validateExplicitId(String key, String id, StreamValue streamValue) {
        // First check if ID is 0-0
        if ("0-0".equals(id)) {
            throw new IllegalArgumentException("The ID specified in XADD must be greater than 0-0");
        }

        // Validate ID format
        if (!ValidationUtil.isValidStreamId(id)) {
            throw new IllegalArgumentException("Invalid stream ID format");
        }

        // Check if ID is greater than the last ID - THIS MUST COME BEFORE DUPLICATE CHECK
        Optional<String> lastIdOpt = getLastStreamId(key);
        if (lastIdOpt.isPresent() && ValidationUtil.compareStreamIds(id, lastIdOpt.get()) <= 0) {
            throw new IllegalArgumentException(
                    "The ID specified in XADD is equal or smaller than the target stream top item");
        }

        // Only after checking ordering, check for duplicates
        if (streamValue.value().containsKey(id)) {
            throw new IllegalArgumentException("The ID specified in XADD already exists");
        }

        return id;
    }

    private StoredValue<?> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            return null;
        }
        return value;
    }
}
