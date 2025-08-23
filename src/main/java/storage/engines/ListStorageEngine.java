package storage.engines;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import common.ErrorMessage;
import storage.interfaces.ListStorage;
import storage.types.ListValue;
import storage.types.StoredValue;

public class ListStorageEngine implements ListStorage {
    private final Map<String, StoredValue<?>> store;

    public ListStorageEngine(Map<String, StoredValue<?>> sharedStore) {
        this.store = sharedStore;
    }

    @Override
    public int leftPush(String key, String... values) {
        if (values.length == 0)
            return getListLength(key);

        var result = store.compute(key, (_, existing) -> {
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

        var result = store.compute(key, (_, existing) -> {
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
                    store.remove(key);
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
                    String.format(ErrorMessage.List.COUNT_EXCEEDS_LENGTH, count, list.length()));
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
                    String.format(ErrorMessage.List.COUNT_EXCEEDS_LENGTH, count, list.length()));
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

    private StoredValue<?> getValidValue(String key) {
        StoredValue<?> value = store.get(key);
        if (value != null && value.isExpired()) {
            store.remove(key);
            return null;
        }
        return value;
    }
}
