package events;

public interface EventListener {
    void onDataAdded(String key);

    void onDataRemoved(String key);
}
