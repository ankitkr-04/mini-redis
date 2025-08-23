package events;

public interface StorageEventPublisher {
    void publishDataAdded(String key);

    void publishDataRemoved(String key);
}
