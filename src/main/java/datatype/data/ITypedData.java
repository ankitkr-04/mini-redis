package datatype.data;

import datatype.ExpiringMetadata;
import enums.DataType;

public sealed interface ITypedData permits StringData, ListData, SetData, StreamData {
    ExpiringMetadata metadata();

    DataType type();

    default boolean isExpired() {
        return metadata().isExpired();
    }


}
