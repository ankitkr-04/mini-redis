package storage.interfaces;

import java.util.Optional;
import storage.expiry.ExpiryPolicy;

public interface StringStorage {
    void setString(String key, String value, ExpiryPolicy expiry);

    Optional<String> getString(String key);
}
