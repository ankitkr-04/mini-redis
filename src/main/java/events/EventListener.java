package events;

/**
 * Listener interface for handling data addition and removal events in a
 * key-value store.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public interface EventListener {
    /**
     * Invoked when data is added for the specified key.
     *
     * @param key the key for which data was added
     */
    void onDataAdded(String key);

    /**
     * Invoked when data is removed for the specified key.
     *
     * @param key the key for which data was removed
     */
    void onDataRemoved(String key);
}
