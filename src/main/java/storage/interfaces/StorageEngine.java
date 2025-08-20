package storage.interfaces;

import storage.types.ValueType;

public interface StorageEngine extends StringStorage, ListStorage, StreamStorage {

    // General operations
    boolean exists(String key);

    boolean delete(String key);

    ValueType getType(String key);

    void clear();

    // Internal operations for blocking
    void cleanup(); // Remove expired keys
}
