package storage.interfaces;


import java.util.Map;
import java.util.Optional;
import storage.expiry.ExpiryPolicy;

public interface StreamStorage {
    String addStreamEntry(String key, String id, Map<String, String> fields, ExpiryPolicy expiry);

    Optional<String> getLastStreamId(String key);

}
