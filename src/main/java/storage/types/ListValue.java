package storage.types;

import collections.QuickList;
import storage.expiry.ExpiryPolicy;

/**
 * Represents a Redis-style list value with an associated expiry policy.
 * 
 * @Override
 *           public ValueType type() {
 *           Provides utility methods for creating empty lists and retrieving
 *           list size. return ValueType.LIST;
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 *        The default expiry policy for lists that never expire.
 */
public record ListValue(QuickList<String> list, ExpiryPolicy expiry)
        implements StoredValue<QuickList<String>> {

    private static final ExpiryPolicy DEFAULT_EXPIRY = ExpiryPolicy.never();

    @Override
    public ValueType type() {
        return ValueType.LIST;
    }

    /**
     * Creates an empty list value with the default expiry policy (never expires).
     *
     * @return a new ListValue instance with an empty list and default expiry
     */
    public static ListValue empty() {
        return new ListValue(new QuickList<>(), DEFAULT_EXPIRY);
    }

    /**
     * Creates an empty list value with the specified expiry policy.
     *
     * @param expiryPolicy the expiry policy to associate with the list
     * @return a new ListValue instance with an empty list and the given expiry
     */
    public static ListValue empty(ExpiryPolicy expiryPolicy) {
        return new ListValue(new QuickList<>(), expiryPolicy);
    }

    /**
     * Returns the number of elements in the list.
     *
     * @return the size of the list
     */
    public int size() {
        return list.length();
    }

    /**
     * Returns the underlying QuickList representing the list value.
     * 
     * @return the QuickList of strings
     */
    @Override
    public QuickList<String> value() {
        return list;
    }
}
