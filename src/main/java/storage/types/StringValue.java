package storage.types;

import storage.expiry.ExpiryPolicy;

public record StringValue(String value, ExpiryPolicy expiry) implements StoredValue<String> {

    @Override
    public ValueType type() {
        return ValueType.STRING;
    }

    public static StringValue of(String value) {
        return new StringValue(value, ExpiryPolicy.never());
    }

    public static StringValue of(String value, ExpiryPolicy expiry) {
        return new StringValue(value, expiry);
    }
}
