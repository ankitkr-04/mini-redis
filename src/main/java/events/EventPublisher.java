package events;

/**
 * EventPublisher is an interface for publishing events related to data changes.
 * Implementations should define how events are broadcast when data is added,
 * removed, or modified.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public interface EventPublisher {

    /**
     * Publishes an event indicating that data has been added for the specified key.
     *
     * @param key the key for which data was added
     */
    void publishDataAdded(String key);

    /**
     * Publishes an event indicating that data has been removed for the specified
     * key.
     *
     * @param key the key for which data was removed
     */
    void publishDataRemoved(String key);

    /**
     * Publishes an event indicating that the data for the specified key has been
     * modified.
     *
     * @param key the key for which data was modified
     */
    void publishKeyModified(String key);
}