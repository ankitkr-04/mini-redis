package datatype.data;

import datatype.ExpiringMetadata;
import enums.DataType;

public record StringData(String val, ExpiringMetadata metadata) implements ITypedData {
    @Override
    public DataType type() {
        return DataType.STRING;
    }

    public static StringData of(String val) {
        return new StringData(val, ExpiringMetadata.never());
    }

    public static StringData of(String val, ExpiringMetadata metadata) {
        return new StringData(val, metadata);
    }

}
