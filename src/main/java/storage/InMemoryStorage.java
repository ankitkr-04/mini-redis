package storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import storage.expiry.ExpiryPolicy;
import storage.interfaces.StorageEngine;
import storage.types.ListValue;
import storage.types.StoredValue;
import storage.types.StringValue;
import storage.types.ValueType;
import storage.types.streams.StreamEntry;
import storage.types.streams.StreamValue;

public final class InMemoryStorage implements StorageEngine {
    private final Map<String, StoredValue<?>> store = new ConcurrentHashMap<>();

    @Override
    public void setString(String key, String value, ExpiryPolicy expiry) {
        store.put(key, StringValue.of(value, expiry));
    }

    @Override
    public Optional<String> getString(String key) {
        return switch (getValidValue(key)) {
            case StringValue(var value, var _) -> Optional.of(value);
            case null -> Optional.empty();
            default -> Optional.empty(); // Wrong type
        };
    }

    @Override
    public int leftPush(String key, String... values) {
        if (values.length == 0)
            return getListLength(key);

        var result = store.compute(key, (k, existing) -> {
            return switch (existing) {
                case null -> {
                    var list = ListValue.empty();
                    list.value().pushLeft(values);
                    yield list;
                }
                case ListValue listVal when !listVal.isExpired() -> {
                    listVal.value().pushLeft(values);
                    yield listVal;
                }
                default -> {
                    // Create new list (expired or wrong type)
                    var list = ListValue.empty();
                    list.value().pushLeft(values);
                    yield list;
                }
            };
        });

        return ((ListValue) result).size();
    }

    @Override
    public int rightPush(String key, String... values) {
        if (values.length == 0)
            return getListLength(key);

        var result = store.compute(key, (k, existing) -> {
            return switch (existing) {
                case null -> {
                    var list = ListValue.empty();
                    list.value().pushRight(values);
                    yield list;
                }
                case ListValue listVal when !listVal.isExpired() -> {
                    listVal.value().pushRight(values);
                    yield listVal;
                }
                default -> {
                    var list = ListValue.empty();
                    list.value().pushRight(values);
                    yield list;
                }
            };
        });

        return ((ListValue) result).size();
    }

    @Override
    public Optional<String> leftPop(String key) {
        return switch (getValidValue(key)) {
            case ListValue(var list, var _) when list.length() > 0 -> {
                String result = list.popLeft();
                if (list.length() == 0) {
                    store.remove(key); // Clean up empty list
                }
                yield Optional.ofNullable(result);
            }
            case null -> Optional.empty();
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<String> rightPop(String key) {
        return switch (getValidValue(key)) {
            case ListValue(var list, var _) when list.length() > 0 -> {
                String result = list.popRight();
                if (list.length() == 0) {
                    store.remove(key);
                }
                yield Optional.ofNullable(result);
            }
            case null -> Optional.empty();
            default -> Optional.empty();
        };
    }

    @Override
    public List<String> leftPop(String key, int count) {
        if (count <= 0)
            return List.of();

        return switch (getValidValue(key)) {
            case ListValue(var list, var _) when list.length() >= count -> {
                List<String> result = list.popLeft(count);
                if (list.length() == 0) {
                    store.remove(key);
                }
                yield result;
            }
            case ListValue(var list, var _) -> throw new IndexOutOfBoundsException(
                    "List has " + list.length() + " elements, requested " + count);
            case null -> List.of();
            default -> List.of();
        };
    }

    @Override
    public List<String> rightPop(String key, int count) {
        if (count <= 0)
            return List.of();

        return switch (getValidValue(key)) {
            case ListValue(var list, var _) when list.length() >= count -> {
                List<String> result = list.popRight(count);
                if (list.length() == 0) {
                    store.remove(key);
                }
                yield result;
            }
            case ListValue(var list, var _) -> throw new IndexOutOfBoundsException(
                    "List has " + list.length() + " elements, requested " + count);
            case null -> List.of();
            default -> List.of();
        };
    }

    @Override
    public List<String> getListRange(String key, int start, int end) {
        return switch (getValidValue(key)) {
            case ListValue(var list, var _) -> list.range(start, end);
            case null -> List.of();
            default -> List.of();
        };
    }

    @Override
    public int getListLength(String key) {
        return switch (getValidValue(key)) {
            case ListValue(var list, var _) -> list.length();
            case null -> 0;
            default -> 0;
        };
    }

    // Streams
    @Override
    public String addStreamEntry(String key, String id, Map<String, String> fields,
            ExpiryPolicy expiry) {
        // Create or get existing StreamValue
        StreamValue streamValue = (StreamValue) store.compute(key, (k, existing) -> {
            StreamValue sv = (existing instanceof StreamValue s) ? s
                    : new StreamValue(new java.util.concurrent.ConcurrentSkipListMap<>(), expiry);
            return sv;
        });

        // Handle auto-ID if user passed "*"
        String entryId;
        if ("*".equals(id)) {
            long timestamp = System.currentTimeMillis();
            // Add a sequence to avoid collisions for same timestamp
            long seq = streamValue.value().size() + 1L;
            entryId = timestamp + "-" + seq;
        } else {
            entryId = id;
        }

        // Create entry and put into stream
        StreamEntry entry = new StreamEntry(entryId, fields);
        streamValue.value().put(entryId, entry);

        return entryId;
    }

    @Override
    public boolean exists(String key) {
        return getValidValue(key) != null;
    }

    @Override
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    @Override
    public ValueType getType(String key) {
        var value = getValidValue(key);
        return value != null ? value.type() : ValueType.NONE;
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public void cleanup() {
        // Remove expired keys lazily
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Gets a value if it exists and hasn't expired
     */
    private StoredValue<?> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key); // Lazy cleanup
            return null;
        }
        return value;
    }


}
