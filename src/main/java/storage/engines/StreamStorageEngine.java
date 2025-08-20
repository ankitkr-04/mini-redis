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
        StreamValue streamValue = (StreamValue) store.compute(key, (k, existing) -> {
            if (existing instanceof StreamValue sv && !sv.isExpired())
                return sv;
            return new StreamValue(new ConcurrentSkipListMap<>(), expiry);
        });

        String entryId = generateOrValidateId(key, id, streamValue);
        streamValue.value().put(entryId, new StreamEntry(entryId, fields));
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
            String[] parts = lastIdOpt.get().split("-");
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

        long timestamp;
        try {
            timestamp = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stream ID format");
        }

        long seq = (timestamp == 0) ? 1 : 0;

        Optional<String> lastIdOpt = getLastStreamId(key);
        if (lastIdOpt.isPresent()) {
            String[] lastParts = lastIdOpt.get().split("-");
            long lastMs = Long.parseLong(lastParts[0]);
            long lastSeq = Long.parseLong(lastParts[1]);

            if (lastMs == timestamp)
                seq = lastSeq + 1;
            else if (timestamp < lastMs)
                throw new IllegalArgumentException(
                        "The ID specified in XADD is equal or smaller than the target stream top item");
        }

        return timestamp + "-" + seq;
    }

    private String validateExplicitId(String key, String id, StreamValue streamValue) {
        if ("0-0".equals(id))
            throw new IllegalArgumentException("The ID specified in XADD must be greater than 0-0");

        if (!ValidationUtil.isValidStreamId(id))
            throw new IllegalArgumentException("Invalid stream ID format");

        Optional<String> lastIdOpt = getLastStreamId(key);
        if (lastIdOpt.isPresent() && ValidationUtil.compareStreamIds(id, lastIdOpt.get()) <= 0)
            throw new IllegalArgumentException(
                    "The ID specified in XADD is equal or smaller than the target stream top item");

        if (streamValue.value().containsKey(id))
            throw new IllegalArgumentException("The ID specified in XADD already exists");

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
