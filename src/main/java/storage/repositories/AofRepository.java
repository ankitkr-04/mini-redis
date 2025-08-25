package storage.repositories;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import storage.PersistentRepository;
import storage.types.StoredValue;

public class AofRepository implements PersistentRepository {

    private final Map<String, StoredValue<?>> store;

    public AofRepository(Map<String, StoredValue<?>> store) {
        this.store = store;
    }

    @Override
    public void saveSnapshot(File rdbFile) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveSnapshot'");
    }

    @Override
    public void loadSnapshot(File rdbFile) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadSnapshot'");
    }
    // Implementation for AOF persistence can be added here

}
