package store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ListStore {
    private final Map<String, List<String>> store = new ConcurrentHashMap<>();

    /**
     * Pushes elements to the right (end) of a list. Creates the list if it doesn't exist.
     */
    public int rightPush(String key, String... values) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }

        List<String> list =
                store.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));

        Collections.addAll(list, values);
        return list.size();
    }

    /**
     * Pushes elements to the left (beginning) of a list. Creates the list if it doesn't exist.
     */
    public int leftPush(String key, String... values) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }

        List<String> list =
                store.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()));

        // Add elements in reverse order to maintain correct sequence
        for (int i = values.length - 1; i >= 0; i--) {
            list.add(0, values[i]);
        }

        return list.size();
    }

    /**
     * Gets a range of elements from a list. Supports negative indices (-1 is last element, -2
     * second to last, etc.)
     */
    public List<String> getRange(String key, int start, int end) {
        if (key == null) {
            return Collections.emptyList();
        }

        List<String> list = store.get(key);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        ListIndexRange range = normalizeIndexRange(list.size(), start, end);

        if (!range.isValidRange()) {
            return Collections.emptyList();
        }

        // Return defensive copy to prevent external modification
        return new ArrayList<>(list.subList(range.start(), range.end() + 1));
    }

    /**
     * Gets the length of a list.
     */
    public int getLength(String key) {
        if (key == null) {
            return 0;
        }

        List<String> list = store.get(key);
        return list != null ? list.size() : 0;
    }

    /**
     * Removes and returns elements from the right (end) of a list.
     */
    public Optional<String> rightPop(String key) {
        if (key == null) {
            return Optional.empty();
        }

        List<String> list = store.get(key);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }

        synchronized (list) {
            if (list.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(list.remove(list.size() - 1));
        }
    }

    /**
     * Removes and returns elements from the left (beginning) of a list.
     */
    public Optional<String> leftPop(String key) {
        if (key == null) {
            return Optional.empty();
        }

        List<String> list = store.get(key);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }

        synchronized (list) {
            if (list.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(list.remove(0));
        }
    }

    /**
     * Checks if a list exists.
     */
    public boolean exists(String key) {
        return key != null && store.containsKey(key);
    }

    /**
     * Deletes a list.
     */
    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    /**
     * Clears all lists from the store.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Normalizes start and end indices, handling negative values and boundary conditions.
     */
    private ListIndexRange normalizeIndexRange(int listSize, int start, int end) {
        // Handle negative indices
        if (start < 0) {
            start = listSize + start;
        }
        if (end < 0) {
            end = listSize + end;
        }

        // Clamp to valid range
        start = Math.max(0, start);
        end = Math.min(listSize - 1, end);

        return new ListIndexRange(start, end, listSize);
    }

    /**
     * Helper record for handling index range validation.
     */
    private record ListIndexRange(int start, int end, int listSize) {
        public boolean isValidRange() {
            return start <= end && start < listSize;
        }
    }
}
