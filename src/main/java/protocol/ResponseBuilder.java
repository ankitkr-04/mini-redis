package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ProtocolConstants;

public final class ResponseBuilder {
    static Logger log = LoggerFactory.getLogger(ResponseBuilder.class);

    private ResponseBuilder() {
    }

    public static ByteBuffer simpleString(String message) {
        return encode(String.valueOf(ProtocolConstants.SIMPLE_STRING) + message + ProtocolConstants.CRLF);
    }

    public static ByteBuffer error(String message) {
        return encode(String.valueOf(ProtocolConstants.ERROR) + "ERR " + message + ProtocolConstants.CRLF);
    }

    public static ByteBuffer integer(long value) {
        return encode(String.valueOf(ProtocolConstants.INTEGER) + value + ProtocolConstants.CRLF);
    }

    public static ByteBuffer bulkString(String value) {
        if (value == null) {
            return encode(ProtocolConstants.RESP_NULL_BULK_STRING);
        }
        return encode(String.valueOf(ProtocolConstants.BULK_STRING) + value.length() + ProtocolConstants.CRLF + value
                + ProtocolConstants.CRLF);
    }

    public static ByteBuffer array(List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode(ProtocolConstants.RESP_EMPTY_ARRAY);
        }

        StringBuilder response = new StringBuilder();
        response.append(ProtocolConstants.ARRAY).append(elements.size()).append(ProtocolConstants.CRLF);
        for (String str : elements) {
            response.append(asString(bulkString(str)));
        }
        return encode(response.toString());
    }

    public static ByteBuffer arrayOfBuffers(List<ByteBuffer> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode(ProtocolConstants.RESP_EMPTY_ARRAY);
        }

        int totalSize = String.valueOf(ProtocolConstants.ARRAY).length() + String.valueOf(elements.size()).length()
                + ProtocolConstants.CRLF.length();
        for (ByteBuffer buf : elements) {
            totalSize += buf.remaining();
        }

        ByteBuffer result = ByteBuffer.allocate(totalSize);
        result.put(
                (String.valueOf(ProtocolConstants.ARRAY) + elements.size() + ProtocolConstants.CRLF).getBytes(StandardCharsets.UTF_8));
        for (ByteBuffer buf : elements) {
            result.put(buf.duplicate());
        }
        result.flip();
        return result;
    }

    public static <T> ByteBuffer arrayWith(List<T> elements, Function<T, String> mapper) {
        if (elements == null || elements.isEmpty()) {
            return encode(ProtocolConstants.RESP_EMPTY_ARRAY);
        }

        StringBuilder response = new StringBuilder();
        response.append(ProtocolConstants.ARRAY).append(elements.size()).append(ProtocolConstants.CRLF);
        for (T element : elements) {
            String value = mapper.apply(element);
            response.append(asString(bulkString(value)));
        }
        return encode(response.toString());
    }

    public static <T> ByteBuffer streamEntries(List<T> entries, Function<T, String> idExtractor,
            Function<T, List<String>> fieldsExtractor) {
        if (entries == null || entries.isEmpty()) {
            return encode(ProtocolConstants.RESP_EMPTY_ARRAY);
        }

        StringBuilder response = new StringBuilder();
        response.append(ProtocolConstants.ARRAY).append(entries.size()).append(ProtocolConstants.CRLF);
        for (T entry : entries) {
            String id = idExtractor.apply(entry);
            List<String> fields = fieldsExtractor.apply(entry);

            response.append(ProtocolConstants.ARRAY).append(2).append(ProtocolConstants.CRLF);
            response.append(asString(bulkString(id)));
            response.append(ProtocolConstants.ARRAY).append(fields.size()).append(ProtocolConstants.CRLF);
            for (String field : fields) {
                response.append(asString(bulkString(field)));
            }
        }
        return encode(response.toString());
    }

    public static ByteBuffer encode(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String asString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // --- Replication specific responses ---
    public static String fullResync(String replId, long offset) {
        return String.valueOf(ProtocolConstants.SIMPLE_STRING)
                + ProtocolConstants.FULLRESYNC_PREFIX + " "
                + replId + " "
                + offset
                + ProtocolConstants.CRLF;
    }

    public static String continueResync() {
        return String.valueOf(ProtocolConstants.SIMPLE_STRING)
                + ProtocolConstants.CONTINUE_PREFIX
                + ProtocolConstants.CRLF;
    }

    // --- returns a ByteBuffer containing e.g. "+FULLRESYNC <id> <offset>\r\n" ---
    public static ByteBuffer fullResyncBuffer(String replId, long offset) {
        String header = String.valueOf(ProtocolConstants.SIMPLE_STRING)
                + ProtocolConstants.FULLRESYNC_PREFIX + " "
                + replId + " "
                + offset
                + ProtocolConstants.CRLF;
        return encode(header);
    }

    // --- returns a ByteBuffer containing a RESP bulk string for binary payload ---
    // Format: $<length>\r\n<binary bytes>\r\n
    public static ByteBuffer bulkStringFromBytes(byte[] data) {
        if (data == null) {
            // $-1\r\n
            return encode(ProtocolConstants.RESP_NULL_BULK_STRING);
        }

        // Build header "$<len>\r\n"
        byte[] header = (String.valueOf(ProtocolConstants.BULK_STRING) + data.length + ProtocolConstants.CRLF)
                .getBytes(StandardCharsets.ISO_8859_1);

        byte[] tail = ProtocolConstants.CRLF.getBytes(StandardCharsets.ISO_8859_1);

        // allocate header + data + tail
        ByteBuffer buf = ByteBuffer.allocate(header.length + data.length + tail.length);
        buf.put(header);
        buf.put(data);
        buf.flip();
        return buf;
    }

    public static ByteBuffer rdbFilePayload(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("RDB file cannot be null");
        }

        // Build header "$<len>\r\n"
        byte[] header = (String.valueOf(ProtocolConstants.BULK_STRING) + data.length + ProtocolConstants.CRLF)
                .getBytes(StandardCharsets.ISO_8859_1);

        log.debug("RDB file payload header: {}", new String(header, StandardCharsets.ISO_8859_1));
        log.debug("RDB file payload data length: {}", data.length);

        // Allocate header + raw data (⚠️ no trailing CRLF)
        ByteBuffer buf = ByteBuffer.allocate(header.length + data.length);
        buf.put(header);
        buf.put(data);
        buf.flip();

        return buf;
    }

}