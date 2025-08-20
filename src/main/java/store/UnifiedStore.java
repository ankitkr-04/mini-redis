package store;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import datatype.ExpiringMetadata;
import datatype.data.ITypedData;
import datatype.data.ListData;
import datatype.data.StringData;
import enums.DataType;

public final class UnifiedStore {


    private final Map<String, ITypedData> store = new ConcurrentHashMap<>();

    public Optional<ITypedData> get(String key) {
        if (key == null)
            return Optional.empty();

        ITypedData data = store.get(key);
        if (data == null)
            return Optional.empty();

        if (data.isExpired()) {
            store.remove(key); // Lazy cleanup
            return Optional.empty();
        }

        return Optional.of(data);
    }

    public boolean exists(String key) {
        return get(key).isPresent();
    }

    public Optional<DataType> getType(String key) {
        return get(key).map(ITypedData::type);
    }

    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    public void clear() {
        store.clear();
    }

    public void setString(String key, String value) {
        store.put(key, StringData.of(value));
    }

    public void setString(String key, String value, ExpiringMetadata metadata) {
        store.put(key, StringData.of(value, metadata));
    }

    public Optional<String> getString(String key) {
        return switch (get(key).orElse(null)) {
            case null -> Optional.empty();
            case StringData(var value, var meta) -> Optional.of(value);
            default -> Optional.empty(); // Default case
        };
    }


    // --- List operations ---
    public int leftPush(String key, String... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }

        ITypedData result = store.compute(key, (k, existing) -> {
            return switch (existing) {
                case null -> {
                    ListData newList = ListData.empty();
                    newList.list().pushLeft(values);
                    yield newList;
                }
                case ListData(var list, var meta) when !meta.isExpired() -> {
                    list.pushLeft(values);
                    yield existing;
                }
                case ListData(var list, var meta) when meta.isExpired() -> {
                    ListData newList = ListData.empty();
                    newList.list().pushLeft(values);
                    yield newList;
                }
                default -> throw new IllegalArgumentException(
                        "Key exists but is not a list, type: " + existing.type());
            };
        });

        return switch (result) {
            case ListData(var list, var meta) -> list.length();
            default -> throw new IllegalStateException("Unexpected type after compute");
        };
    }

    public int rightPush(String key, String... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }

        ITypedData result = store.compute(key, (k, existing) -> {
            return switch (existing) {
                case null -> {
                    ListData newList = ListData.empty();
                    newList.list().pushRight(values);
                    yield newList;
                }
                case ListData(var list, var meta) when !meta.isExpired() -> {
                    list.pushRight(values);
                    yield existing;
                }
                case ListData(var list, var meta) when meta.isExpired() -> {
                    ListData newList = ListData.empty();
                    newList.list().pushRight(values);
                    yield newList;
                }
                default -> throw new IllegalArgumentException(
                        "Key exists but is not a list, type: " + existing.type());
            };
        });

        return switch (result) {
            case ListData(var list, var meta) -> list.length();
            default -> throw new IllegalStateException("Unexpected type after compute");
        };
    }

    public Optional<String> leftPop(String key) {
        return switch (get(key).orElse(null)) {
            case null -> Optional.empty();
            case ListData(var list, var meta) when list.length() > 0 -> {
                String result = list.popLeft();
                if (list.length() == 0) {
                    store.remove(key); // Remove empty list
                }
                yield Optional.ofNullable(result);
            }
            case ListData(var list, var meta) -> Optional.empty(); // Empty list
            default -> Optional.empty(); // Wrong type
        };
    }

    public Optional<String> rightPop(String key) {
        return switch (get(key).orElse(null)) {
            case null -> Optional.empty();
            case ListData(var list, var meta) when list.length() > 0 -> {
                String result = list.popRight();
                if (list.length() == 0) {
                    store.remove(key); // Remove empty list
                }
                yield Optional.ofNullable(result);
            }
            case ListData(var list, var meta) -> Optional.empty(); // Empty list
            default -> Optional.empty(); // Wrong type
        };
    }

    public List<String> leftPop(String key, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        if (count == 0) {
            return List.of();
        }

        return switch (get(key).orElse(null)) {
            case null -> List.of();
            case ListData(var list, var meta) when list.length() >= count -> {
                List<String> result = list.popLeft(count);
                if (list.length() == 0) {
                    store.remove(key); // Remove empty list
                }
                yield result;
            }
            case ListData(var list, var meta) -> {
                throw new IndexOutOfBoundsException(
                        "Requested " + count + " elements, but list only has " + list.length());
            }
            default -> List.of(); // Wrong type
        };
    }

    public List<String> getListRange(String key, int start, int end) {
        return switch (get(key).orElse(null)) {
            case null -> List.of();
            case ListData(var list, var meta) -> list.range(start, end);
            default -> List.of(); // Wrong type
        };
    }

    public int getListLength(String key) {
        return switch (get(key).orElse(null)) {
            case null -> 0;
            case ListData(var list, var meta) -> list.length();
            default -> 0; // Wrong type
        };
    }


}
