package storage.persistence;

import java.io.File;
import java.io.IOException;

/**
 * Interface for repository persistence operations such as saving and loading
 * snapshots.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public interface PersistentRepository {

    /**
     * Saves the current state to the specified RDB (Redis Database) file.
     *
     * @param rdbFile the file to which the snapshot will be saved
     */
    void saveSnapshot(File rdbFile);

    /**
     * Loads the state from the specified RDB (Redis Database) file.
     *
     * @param rdbFile the file from which the snapshot will be loaded
     * @throws IOException if an I/O error occurs during loading
     */
    void loadSnapshot(File rdbFile) throws IOException;
}
