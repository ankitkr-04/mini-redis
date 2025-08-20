package datatype.data;

import datatype.ExpiringMetadata;
import datatype.QuickList;
import enums.DataType;

public record ListData(QuickList<String> list, ExpiringMetadata metadata) implements ITypedData {
    @Override
    public DataType type() {
        return DataType.LIST;
    }

    public static ListData empty() {
        return new ListData(new QuickList<>(), ExpiringMetadata.never());
    }

    public static ListData empty(ExpiringMetadata metadata) {
        return new ListData(new QuickList<>(), metadata);
    }

    public int size() {
        return list.length();
    }
}
