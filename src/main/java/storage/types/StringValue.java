package storage.types;

import storage.expiry.ExpiryPolicy;

/**
 * Represents a string value stored in the system with an associated expiry
 * policy.
 * Provides factory methods for creating instances with or without expiry.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public record StringValue(String stringValue, ExpiryPolicy expiryPolicy) implements StoredValue<String> {

    private static final ValueType VALUE_TYPE = ValueType.STRING;

    /**
     * Returns the type of value stored.
     *
     * @return ValueType.STRING
     */
    @Override
    public ValueType type() {
        return VALUE_TYPE;
    }

    /**
     * Creates a StringValue with no expiry.
     *
     * @param stringValue the string to store
     * @return a new StringValue instance with no expiry
     */
    public static StringValue of(String stringValue) {

        return new StringValue(stringValue, ExpiryPolicy.never());
    }

    /**
     * Creates a StringValue with a specified expiry policy.
     *
     * @param stringValue  the string to store
     * @param expiryPolicy the expiry policy to apply
     * @return a new StringValue instance with the given expiry policy
     */
    public static StringValue of(String stringValue, ExpiryPolicy expiryPolicy) {

        return new StringValue(stringValue, expiryPolicy);
    }

    /**
     * Returns the actual stored string value.
     * 
     * @return the stored string value
     */
    @Override
    public String value() {
        return stringValue;
    }

    /**
     * Returns the expiry policy associated with this string value.
     * 
     * @return the expiry policy
     */

    @Override
    public ExpiryPolicy expiry() {
        return expiryPolicy;
    }
}
