package storage.types;

import collections.QuickList;
import storage.expiry.ExpiryPolicy;

public record ListValue(QuickList<String> value, ExpiryPolicy expiry)
        implements StoredValue<QuickList<String>> {

    @Override
    public ValueType type() {
        return ValueType.LIST;
    }

    public static ListValue empty() {
        return new ListValue(new QuickList<>(), ExpiryPolicy.never());
    }

    public static ListValue empty(ExpiryPolicy expiry) {
        return new ListValue(new QuickList<>(), expiry);
    }

    public int size() {
        return value.length();
    }
}
