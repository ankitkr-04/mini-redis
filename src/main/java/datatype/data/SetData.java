package datatype.data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import datatype.ExpiringMetadata;
import enums.DataType;

public record SetData(Set<String> set, ExpiringMetadata metadata) implements ITypedData {

    @Override
    public DataType type() {
        return DataType.SET;
    }

    public static SetData empty() {
        return new SetData(ConcurrentHashMap.newKeySet(), ExpiringMetadata.never());
    }

    public static SetData empty(ExpiringMetadata metadata) {
        return new SetData(ConcurrentHashMap.newKeySet(), metadata);
    }

    public int size() {
        return set.size();
    }
}
