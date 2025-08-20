package store;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import util.QuickList;

public final class ListStore {
    private final Map<String, QuickList<String>> store = new ConcurrentHashMap<>();

    /** Push elements to the right (end). */
    public int rightPush(String key, String... values) {
        validateKeyAndValues(key, values);
        QuickList<String> list = store.computeIfAbsent(key, k -> new QuickList<>());
        list.pushRight(values);
        return list.length();
    }

    /** Push elements to the left (beginning). */
    public int leftPush(String key, String... values) {
        validateKeyAndValues(key, values);
        QuickList<String> list = store.computeIfAbsent(key, k -> new QuickList<>());
        list.pushLeft(values);
        return list.length();
    }

    /** Pop a single element from the right. */
    public Optional<String> rightPop(String key) {
        if (key == null)
            return Optional.empty();
        QuickList<String> list = store.get(key);
        if (list == null || list.length() == 0)
            return Optional.empty();
        return Optional.ofNullable(list.popRight());
    }

    /** Pop a single element from the left. */
    public Optional<String> leftPop(String key) {
        if (key == null)
            return Optional.empty();
        QuickList<String> list = store.get(key);
        if (list == null || list.length() == 0)
            return Optional.empty();
        return Optional.ofNullable(list.popLeft());
    }

    /** Pop multiple elements from the left efficiently. */
    public List<String> leftPop(String key, int count) {
        if (count < 0)
            throw new IllegalArgumentException("elementCount cannot be negative");

        QuickList<String> list = store.get(key);
        if (key == null || list == null || list.length() == 0)
            return Collections.emptyList();

        if (list.length() < count)
            throw new IndexOutOfBoundsException(
                    "Requested " + count + " elements, but list only has " + list.length());

        // QuickList supports bulk range extraction and removal
        List<String> removed = list.range(0, count - 1);
        for (int i = 0; i < count; i++) {
            list.popLeft();
        }
        return removed;
    }

    /** Pop multiple elements from the right efficiently. */
    public List<String> rightPop(String key, int count) {
        if (count < 0)
            throw new IllegalArgumentException("elementCount cannot be negative");

        QuickList<String> list = store.get(key);
        if (key == null || list == null || list.length() == 0)
            return Collections.emptyList();

        if (list.length() < count)
            throw new IndexOutOfBoundsException(
                    "Requested " + count + " elements, but list only has " + list.length());

        int len = list.length();
        List<String> removed = list.range(len - count, len - 1);
        for (int i = 0; i < count; i++) {
            list.popRight();
        }
        return removed;
    }

    /** Get elements in a range. */
    public List<String> getRange(String key, int start, int end) {
        if (key == null)
            return Collections.emptyList();
        QuickList<String> list = store.get(key);
        if (list == null || list.length() == 0)
            return Collections.emptyList();
        return list.range(start, end);
    }

    /** Get length of a list. */
    public int getLength(String key) {
        if (key == null)
            return 0;
        QuickList<String> list = store.get(key);
        return list != null ? list.length() : 0;
    }

    /** Check existence of a list. */
    public boolean exists(String key) {
        return key != null && store.containsKey(key);
    }

    /** Delete a list. */
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    /** Clear all lists. */
    public void clear() {
        store.clear();
    }

    private void validateKeyAndValues(String key, String... values) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null");
        if (values == null || values.length == 0)
            throw new IllegalArgumentException("Values cannot be null or empty");
    }
}
