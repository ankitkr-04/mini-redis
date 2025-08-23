package storage.repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import errors.ErrorCode;
import storage.Repository;
import storage.expiry.ExpiryPolicy;
import storage.types.StoredValue;
import storage.types.ValueType;
import storage.types.streams.StreamEntry;
import storage.types.streams.StreamRangeEntry;
import storage.types.streams.StreamValue;
import utils.StreamIdComparator;

public final class StreamRepository
        implements Repository<ConcurrentNavigableMap<String, StreamEntry>> {
    private final Map<String, StoredValue<?>> store;

    public StreamRepository(Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void put(String key, ConcurrentNavigableMap<String, StreamEntry> value,
            ExpiryPolicy expiry) {
        store.put(key, new StreamValue(value, expiry));
    }

    @Override
    public Optional<ConcurrentNavigableMap<String, StreamEntry>> get(String key) {
        return getValidValue(key)
                .filter(StreamValue.class::isInstance)
                .map(StreamValue.class::cast)
                .map(StreamValue::value);
    }

    @Override
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public boolean exists(String key) {
        return getValidValue(key).isPresent();
    }

    @Override
    public ValueType getType(String key) {
        return getValidValue(key)
                .map(StoredValue::type)
                .orElse(ValueType.NONE);
    }

    // Stream-specific operations
    public ConcurrentNavigableMap<String, StreamEntry> getOrCreate(String key,
            ExpiryPolicy expiry) {
        return get(key).orElseGet(() -> {
            ConcurrentNavigableMap<String, StreamEntry> newStream =
                    new ConcurrentSkipListMap<>(StreamIdComparator.INSTANCE);
            put(key, newStream, expiry);
            return newStream;
        });
    }

    public String addEntry(String key, String id, Map<String, String> fields, ExpiryPolicy expiry) {
        ConcurrentNavigableMap<String, StreamEntry> stream = getOrCreate(key, expiry);
        String entryId = generateOrValidateId(key, id, stream);
        stream.put(entryId, new StreamEntry(entryId, fields));
        return entryId;
    }

    public Optional<String> getLastId(String key) {
        return get(key)
                .filter(stream -> !stream.isEmpty())
                .map(ConcurrentNavigableMap::lastKey);
    }

    public List<StreamRangeEntry> getRange(String key, String start, String end, int count) {
        return get(key).map(stream -> {
            if (stream.isEmpty())
                return List.<StreamRangeEntry>of();

            String actualStart = normalizeRangeStart(start, stream);
            String actualEnd = normalizeRangeEnd(end, stream);

            var rangeMap = stream.subMap(actualStart, true, actualEnd, true);

            return rangeMap.values().stream()
                    .limit(count > 0 ? count : Long.MAX_VALUE)
                    .map(this::convertToRangeEntry)
                    .collect(Collectors.toList());
        }).orElse(List.of());
    }

    public List<StreamRangeEntry> getAfter(String key, String afterId, int count) {
        return get(key).map(stream -> {
            if (stream.isEmpty())
                return List.<StreamRangeEntry>of();

            var rangeMap = stream.tailMap(afterId, false);

            return rangeMap.values().stream()
                    .limit(count > 0 ? count : Long.MAX_VALUE)
                    .map(this::convertToRangeEntry)
                    .collect(Collectors.toList());
        }).orElse(List.of());
    }

    private String generateOrValidateId(String key, String id,
            ConcurrentNavigableMap<String, StreamEntry> stream) {
        return switch (id) {
            case "*" -> generateAutoId(key, stream);
            case String s when s.endsWith("-*") -> generateTimestampId(key, s, stream);
            default -> validateExplicitId(id, stream);
        };
    }

    private String generateAutoId(String key, ConcurrentNavigableMap<String, StreamEntry> stream) {
        long timestamp = System.currentTimeMillis();
        long seq = 0L;

        if (!stream.isEmpty()) {
            String[] parts = stream.lastKey().split("-");
            long lastMs = Long.parseLong(parts[0]);
            long lastSeq = Long.parseLong(parts[1]);

            if (timestamp <= lastMs) {
                timestamp = lastMs;
                seq = lastSeq + 1;
            }
        }
        return timestamp + "-" + seq;
    }

    private String generateTimestampId(String key, String id,
            ConcurrentNavigableMap<String, StreamEntry> stream) {
        String[] parts = id.split("-");
        if (parts.length != 2 || !"*".equals(parts[1])) {
            throw new IllegalArgumentException(ErrorCode.INVALID_STREAM_ID.getMessage());
        }

        long timestamp = Long.parseLong(parts[0]);
        long seq = 0L;

        if (!stream.isEmpty()) {
            String[] lastParts = stream.lastKey().split("-");
            long lastMs = Long.parseLong(lastParts[0]);
            long lastSeq = Long.parseLong(lastParts[1]);

            if (timestamp == 0) {
                if (lastMs == 0) {
                    seq = lastSeq + 1;
                } else {
                    throw new IllegalArgumentException(ErrorCode.STREAM_ID_TOO_SMALL.getMessage());
                }
            } else {
                if (lastMs == timestamp) {
                    seq = lastSeq + 1;
                } else if (timestamp < lastMs) {
                    throw new IllegalArgumentException(ErrorCode.STREAM_ID_TOO_SMALL.getMessage());
                }
            }
        } else if (timestamp == 0) {
            seq = 1;
        }

        return timestamp + "-" + seq;
    }

    private String validateExplicitId(String id,
            ConcurrentNavigableMap<String, StreamEntry> stream) {
        if ("0-0".equals(id)) {
            throw new IllegalArgumentException(ErrorCode.STREAM_ID_ZERO.getMessage());
        }

        if (!stream.isEmpty() && StreamIdComparator.compareIds(id, stream.lastKey()) <= 0) {
            throw new IllegalArgumentException(ErrorCode.STREAM_ID_TOO_SMALL.getMessage());
        }

        if (stream.containsKey(id)) {
            throw new IllegalArgumentException(ErrorCode.STREAM_ID_EXISTS.getMessage());
        }

        return id;
    }

    private String normalizeRangeStart(String start,
            ConcurrentNavigableMap<String, StreamEntry> stream) {
        return switch (start) {
            case "-" -> stream.firstKey();
            case "+" -> throw new IllegalArgumentException("Invalid range: start cannot be '+'");
            default -> start;
        };
    }

    private String normalizeRangeEnd(String end,
            ConcurrentNavigableMap<String, StreamEntry> stream) {
        return switch (end) {
            case "+" -> stream.lastKey();
            case "-" -> throw new IllegalArgumentException("Invalid range: end cannot be '-'");
            default -> end;
        };
    }

    private StreamRangeEntry convertToRangeEntry(StreamEntry entry) {
        List<String> fieldList = entry.fields().entrySet().stream()
                .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return new StreamRangeEntry(entry.id(), fieldList);
    }

    private Optional<StoredValue<?>> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(value);
    }
}
