package storage.repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import errors.ErrorCode;
import storage.Repository;
import storage.expiry.ExpiryPolicy;
import storage.types.StoredValue;
import storage.types.ValueType;
import storage.types.streams.StreamEntry;
import storage.types.streams.StreamRangeEntry;
import storage.types.streams.StreamValue;
import utils.StreamIdComparator;

/**
 * StreamRepository implementation.
 *
 * <p>
 * Storage layer implementation for data persistence.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */

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
        return getValidValue(key).filter(v -> v.type() == ValueType.STREAM)
                .map(v -> ((StreamValue) v).value());
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
        return getValidValue(key).map(StoredValue::type).orElse(ValueType.NONE);
    }

    public ConcurrentNavigableMap<String, StreamEntry> getOrCreate(String key,
            ExpiryPolicy expiry) {
        return get(key).orElseGet(() -> {
            ConcurrentNavigableMap<String, StreamEntry> newStream = new ConcurrentSkipListMap<>(
                    StreamIdComparator.INSTANCE);
            put(key, newStream, expiry);
            return newStream;
        });
    }

    public String addEntry(String key, String id, Map<String, String> fields, ExpiryPolicy expiry) {
        ConcurrentNavigableMap<String, StreamEntry> stream = getOrCreate(key, expiry);
        String entryId = generateOrValidateId(id, stream);
        stream.put(entryId, new StreamEntry(entryId, fields));
        return entryId;
    }

    public Optional<String> getLastId(String key) {
        return get(key).filter(s -> !s.isEmpty()).map(ConcurrentNavigableMap::lastKey);
    }

    public List<StreamRangeEntry> getRange(String key, String start, String end, int count) {
        return get(key).map(stream -> {
            if (stream.isEmpty())
                return List.<StreamRangeEntry>of();
            String actualStart = "-".equals(start) ? stream.firstKey() : start;
            String actualEnd = "+".equals(end) ? stream.lastKey() : end;
            var rangeMap = stream.subMap(actualStart, true, actualEnd, true);
            return rangeMap.values().stream()
                    .limit(count > 0 ? count : Long.MAX_VALUE)
                    .map(this::toRangeEntry)
                    .toList();
        }).orElse(List.of());
    }

    public List<StreamRangeEntry> getAfter(String key, String afterId, int count) {
        return get(key).map(stream -> {
            if (stream.isEmpty())
                return List.<StreamRangeEntry>of();
            var rangeMap = stream.tailMap(afterId, false);
            return rangeMap.values().stream()
                    .limit(count > 0 ? count : Long.MAX_VALUE)
                    .map(this::toRangeEntry)
                    .toList();
        }).orElse(List.of());
    }

    private String generateOrValidateId(String id,
            ConcurrentNavigableMap<String, StreamEntry> stream) {
        if ("*".equals(id))
            return generateAutoId(stream);
        if (id.endsWith("-*"))
            return generateTimestampId(id, stream);
        return validateExplicitId(id, stream);
    }

    private String generateAutoId(ConcurrentNavigableMap<String, StreamEntry> stream) {
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

    private String generateTimestampId(String id,
            ConcurrentNavigableMap<String, StreamEntry> stream) {
        String[] parts = id.split("-");
        long timestamp = Long.parseLong(parts[0]);
        long seq = 0L;
        if (!stream.isEmpty()) {
            String[] lastParts = stream.lastKey().split("-");
            long lastMs = Long.parseLong(lastParts[0]);
            long lastSeq = Long.parseLong(lastParts[1]);
            if (timestamp == 0) {
                if (lastMs == 0)
                    seq = lastSeq + 1;
                else
                    throw new IllegalArgumentException(ErrorCode.STREAM_ID_TOO_SMALL.getMessage());
            } else {
                if (lastMs == timestamp)
                    seq = lastSeq + 1;
                else if (timestamp < lastMs)
                    throw new IllegalArgumentException(ErrorCode.STREAM_ID_TOO_SMALL.getMessage());
            }
        } else if (timestamp == 0)
            seq = 1;
        return timestamp + "-" + seq;
    }

    private String validateExplicitId(String id,
            ConcurrentNavigableMap<String, StreamEntry> stream) {
        if ("0-0".equals(id))
            throw new IllegalArgumentException(ErrorCode.STREAM_ID_ZERO.getMessage());
        if (!stream.isEmpty() && StreamIdComparator.compareIds(id, stream.lastKey()) <= 0)
            throw new IllegalArgumentException(ErrorCode.STREAM_ID_TOO_SMALL.getMessage());
        if (stream.containsKey(id))
            throw new IllegalArgumentException(ErrorCode.STREAM_ID_EXISTS.getMessage());
        return id;
    }

    private StreamRangeEntry toRangeEntry(StreamEntry entry) {
        List<String> fieldList = entry.fields().entrySet().stream()
                .flatMap(e -> List.of(e.getKey(), e.getValue()).stream()).toList();
        return new StreamRangeEntry(entry.id(), fieldList);
    }

    private Optional<StoredValue<?>> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            value = null;
        }
        return Optional.ofNullable(value);
    }
}
