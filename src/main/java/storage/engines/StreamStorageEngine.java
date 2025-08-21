package storage.engines;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import common.ErrorMessage;
import common.ValidationUtil;
import storage.expiry.ExpiryPolicy;
import storage.interfaces.StreamStorage;
import storage.types.StoredValue;
import storage.types.streams.StreamEntry;
import storage.types.streams.StreamRangeEntry;
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

    @Override
    public List<StreamRangeEntry> getStreamRange(String key, String start, String end, int count) {
        StoredValue<?> value = getValidValue(key);
        if (!(value instanceof StreamValue sv)) {
            return List.of(); // Return empty list if stream doesn't exist
        }

        var streamData = sv.value();
        if (streamData.isEmpty()) {
            return List.of();
        }

        // Handle special range markers
        String actualStart = normalizeRangeStart(start, streamData);
        String actualEnd = normalizeRangeEnd(end, streamData);

        // Get entries within range
        var rangeMap = streamData.subMap(actualStart, true, actualEnd, true);

        return rangeMap.values().stream().limit(count > 0 ? count : Long.MAX_VALUE)
                .map(this::convertToRangeEntry).collect(Collectors.toList());
    }

    @Override
    public List<StreamRangeEntry> getStreamRange(String key, String start, String end) {
        return getStreamRange(key, start, end, 0); // 0 means no limit
    }

    private String normalizeRangeStart(String start,
            ConcurrentNavigableMap<String, StreamEntry> streamData) {
        return switch (start) {
            case "-" -> streamData.firstKey(); // Start from the beginning
            case "+" -> throw new IllegalArgumentException("Invalid range: start cannot be '+'");
            default -> {
                if (!ValidationUtil.isValidStreamId(start)) {
                    throw new IllegalArgumentException("Invalid stream ID format: " + start);
                }
                yield start;
            }
        };
    }

    private String normalizeRangeEnd(String end,
            ConcurrentNavigableMap<String, StreamEntry> streamData) {
        return switch (end) {
            case "+" -> streamData.lastKey(); // End at the last entry
            case "-" -> throw new IllegalArgumentException("Invalid range: end cannot be '-'");
            default -> {
                if (!ValidationUtil.isValidStreamId(end)) {
                    throw new IllegalArgumentException("Invalid stream ID format: " + end);
                }
                yield end;
            }
        };
    }

    private StreamRangeEntry convertToRangeEntry(StreamEntry entry) {
        // Convert the Map<String, String> fields to List<String> format
        // Redis XRANGE returns fields as alternating field-value pairs
        List<String> fieldList = entry.fields().entrySet().stream()
                .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new StreamRangeEntry(entry.id(), fieldList);
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
        if (parts.length != 2 || !"*".equals(parts[1])) {
            throw new IllegalArgumentException(ErrorMessage.Stream.INVALID_STREAM_ID_FORMAT);
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ErrorMessage.Stream.INVALID_STREAM_ID_FORMAT);
        }

        Optional<String> lastIdOpt = getLastStreamId(key);
        long seq = 0L;
        if (lastIdOpt.isPresent()) {
            String[] lastParts = lastIdOpt.get().split("-");
            long lastMs = Long.parseLong(lastParts[0]);
            long lastSeq = Long.parseLong(lastParts[1]);

            if (timestamp == 0) {
                if (lastMs == 0) {
                    seq = lastSeq + 1;
                } else {
                    throw new IllegalArgumentException(
                            ErrorMessage.XAdd.ID_EQUAL_OR_SMALLER_THAN_LAST);
                }
            } else {
                if (lastMs == timestamp) {
                    seq = lastSeq + 1;
                } else if (timestamp < lastMs) {
                    throw new IllegalArgumentException(
                            ErrorMessage.XAdd.ID_EQUAL_OR_SMALLER_THAN_LAST);
                }
            }
        } else if (timestamp == 0) {
            seq = 1;
        }

        return timestamp + "-" + seq;
    }

    private String validateExplicitId(String key, String id, StreamValue streamValue) {
        if ("0-0".equals(id))
            throw new IllegalArgumentException(ErrorMessage.XAdd.ID_MUST_BE_GREATER_THAN_0_0);

        if (!ValidationUtil.isValidStreamId(id))
            throw new IllegalArgumentException(ErrorMessage.Stream.INVALID_STREAM_ID_FORMAT);

        Optional<String> lastIdOpt = getLastStreamId(key);
        if (lastIdOpt.isPresent() && ValidationUtil.compareStreamIds(id, lastIdOpt.get()) <= 0)
            throw new IllegalArgumentException(ErrorMessage.XAdd.ID_EQUAL_OR_SMALLER_THAN_LAST);

        if (streamValue.value().containsKey(id))
            throw new IllegalArgumentException(ErrorMessage.XAdd.ID_ALREADY_EXISTS);

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
