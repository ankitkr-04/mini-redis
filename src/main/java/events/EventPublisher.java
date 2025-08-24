package events;

public interface EventPublisher {
    void publishDataAdded(String key);
    void publishDataRemoved(String key);
}