package storage.repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import collections.QuickList;
import storage.Repository;
import storage.expiry.ExpiryPolicy;
import storage.types.ListValue;
import storage.types.StoredValue;
import storage.types.ValueType;

/**
 * Repository for managing Redis-like list data structures.
 * Provides methods for list operations such as push, pop, and range queries.
 */
public final class ListRepository implements Repository<QuickList<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListRepository.class);

    /** Represents an empty list length. */
    private static final int EMPTY_LIST_LENGTH = 0;

    private final Map<String, StoredValue<?>> store;

    public ListRepository(Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void put(String key, QuickList<String> value, ExpiryPolicy expiry) {
        store.put(key, new ListValue(value, expiry));
        LOGGER.debug("Put list for key: {}", key);
    }

    @Override
    public Optional<QuickList<String>> get(String key) {
        return getValidValue(key)
                .filter(storedValue -> storedValue.type() == ValueType.LIST)
                .map(storedValue -> ((ListValue) storedValue).value());
    }

    @Override
    public boolean delete(String key) {
        boolean removed = store.remove(key) != null;
        if (removed) {
            LOGGER.info("Deleted key: {}", key);
        }
        return removed;
    }

    @Override
    public boolean exists(String key) {
        return getValidValue(key).isPresent();
    }

    @Override
    public ValueType getType(String key) {
        return getValidValue(key).map(StoredValue::type).orElse(ValueType.NONE);
    }

    public QuickList<String> getOrCreate(String key) {
        return get(key).orElseGet(() -> {
            QuickList<String> newList = new QuickList<>();
            put(key, newList);
            return newList;
        });
    }

    public int pushLeft(String key, String... values) {
        if (values.length == 0)
            return getLength(key);
        QuickList<String> list = getOrCreate(key);
        list.pushLeft(values);
        return list.length();
    }

    public int pushRight(String key, String... values) {
        if (values.length == 0)
            return getLength(key);
        QuickList<String> list = getOrCreate(key);
        list.pushRight(values);
        return list.length();
    }

    public Optional<String> popLeft(String key) {
        return get(key).map(list -> {
            String result = list.popLeft();
            if (list.isEmpty())
                delete(key);
            return result;
        });
    }

    public Optional<String> popRight(String key) {
        return get(key).map(list -> {
            String result = list.popRight();
            if (list.isEmpty())
                delete(key);
            return result;
        });
    }

    public List<String> popLeft(String key, int count) {
        return get(key).map(list -> {
            List<String> result = list.popLeft(count);
            if (list.isEmpty())
                delete(key);
            return result;
        }).orElse(List.of());
    }

    public List<String> popRight(String key, int count) {
        return get(key).map(list -> {
            List<String> result = list.popRight(count);
            if (list.isEmpty())
                delete(key);
            return result;
        }).orElse(List.of());
    }

    public List<String> range(String key, int start, int end) {
        return get(key).map(list -> list.range(start, end)).orElse(List.of());
    }

    public int getLength(String key) {
        return get(key).map(QuickList::length).orElse(0);
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
