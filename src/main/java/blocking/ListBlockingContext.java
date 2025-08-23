package blocking;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import protocol.ResponseBuilder;
import storage.StorageService;

public record ListBlockingContext(List<String> keys) implements BlockingContext {

    @Override
    public boolean hasDataAvailable(String key, StorageService storage) {
        return keys.contains(key) && storage.getListLength(key) > 0;
    }

    @Override
    public ByteBuffer buildSuccessResponse(StorageService storage) {
        for (String key : keys) {
            if (storage.getListLength(key) > 0) {
                Optional<String> value = storage.leftPop(key);
                if (value.isPresent()) {
                    return ResponseBuilder.array(List.of(key, value.get()));
                }
            }
        }
        return ResponseBuilder.bulkString(null); // Fallback, shouldn't reach here
    }
}
