package storage.interfaces;


import java.util.Map;
import storage.expiry.ExpiryPolicy;

public interface StreamStorage {
    String addStreamEntry(String key, String id, Map<String, String> fields, ExpiryPolicy expiry);
}
