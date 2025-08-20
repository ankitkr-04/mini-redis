package datatype.data;

import datatype.ExpiringMetadata;
import enums.DataType;

public record StreamData(Object stream, ExpiringMetadata metadata) implements ITypedData {

    @Override
    public DataType type() {
        return DataType.STREAM;
    }

    public static StreamData empty() {
        return new StreamData(null, ExpiringMetadata.never());
    }
}
