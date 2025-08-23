package blocking;

import java.nio.ByteBuffer;
import storage.StorageService;

public interface BlockingContext {
    boolean hasDataAvailable(String key, StorageService storage);

    ByteBuffer buildSuccessResponse(StorageService storage);
}
