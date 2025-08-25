package events;

/**
 * Interface for components that need to be notified when keys are modified.
 * Used for optimistic locking in transactions.
 */
public interface KeyModificationListener {
    /**
     * Called when a key is modified (set, deleted, incremented, etc.)
     * 
     * @param key the key that was modified
     */
    void onKeyModified(String key);
}
