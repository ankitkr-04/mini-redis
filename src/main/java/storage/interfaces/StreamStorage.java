package storage.interfaces;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import storage.expiry.ExpiryPolicy;
import storage.types.streams.StreamRangeEntry;

public interface StreamStorage {
    String addStreamEntry(String key, String id, Map<String, String> fields, ExpiryPolicy expiry);


    Optional<String> getLastStreamId(String key);

    List<StreamRangeEntry> getStreamRange(String key, String start, String end, int count);

    List<StreamRangeEntry> getStreamRange(String key, String start, String end);

    List<StreamRangeEntry> getStreamAfter(String key, String afterId, int count);

    default List<StreamRangeEntry> getStreamAfter(String key, String afterId) {
        return getStreamAfter(key, afterId, -1);
    }

}
