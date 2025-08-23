package events;

public interface StorageEventListener {
    void onDataAdded(String key);

    void onDataRemoved(String key);
}
