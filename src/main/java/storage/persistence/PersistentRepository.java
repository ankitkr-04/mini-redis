package storage.persistence;

import java.io.File;
import java.io.IOException;

public interface PersistentRepository {
    void saveSnapshot(File rdbFile);

    void loadSnapshot(File rdbFile) throws IOException;
}
