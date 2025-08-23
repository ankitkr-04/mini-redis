package blocking;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import protocol.ResponseBuilder;
import storage.StorageService;

public record StreamBlockingContext(List<String> keys, List<String> ids, Optional<Integer> count)
        implements BlockingContext {

    @Override
    public boolean hasDataAvailable(String key, StorageService storage) {
        int idx = keys.indexOf(key);
        if (idx == -1)
            return false;
        String afterId = ids.get(idx);
        return !storage.getStreamAfter(key, afterId, 1).isEmpty();
    }

    @Override
    public ByteBuffer buildSuccessResponse(StorageService storage) {
        List<ByteBuffer> responses = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String afterId = ids.get(i);
            int limit = count.orElse(-1);
            var entries = storage.getStreamAfter(key, afterId, limit);
            if (!entries.isEmpty()) {
                ByteBuffer streamResp = ResponseBuilder.arrayOfBuffers(List.of(
                        ResponseBuilder.bulkString(key),
                        ResponseBuilder.streamEntries(entries, e -> e.id(), e -> e.fieldList())));
                responses.add(streamResp);
            }
        }
        if (responses.isEmpty()) {
            return ResponseBuilder.bulkString(null); // Fallback
        }
        return ResponseBuilder.arrayOfBuffers(responses);
    }
}
