package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import config.ProtocolConstants;

public final class ResponseBuilder {
    private ResponseBuilder() {
    } // Utility class

    // Basic response types
    public static ByteBuffer simpleString(String message) {
        return encode("+" + message + ProtocolConstants.CRLF);
    }

    public static ByteBuffer error(String message) {
        return encode("-ERR " + message + ProtocolConstants.CRLF);
    }

    public static ByteBuffer integer(long value) {
        return encode(":" + value + ProtocolConstants.CRLF);
    }

    public static ByteBuffer bulkString(String value) {
        if (value == null) {
            return encode("$-1" + ProtocolConstants.CRLF);
        }
        return encode("$" + value.length() + ProtocolConstants.CRLF + value + ProtocolConstants.CRLF);
    }

    public static ByteBuffer array(List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode("*0" + ProtocolConstants.CRLF);
        }

        StringBuilder response = new StringBuilder();
        response.append("*").append(elements.size()).append(ProtocolConstants.CRLF);

        for (String element : elements) {
            response.append(bufferToString(bulkString(element)));
        }

        return encode(response.toString());
    }

    public static ByteBuffer arrayOfBuffers(List<ByteBuffer> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode("*0" + ProtocolConstants.CRLF);
        }

        int totalSize = calculateTotalSize(elements);
        ByteBuffer result = ByteBuffer.allocate(totalSize);

        result.put(("*" + elements.size() + ProtocolConstants.CRLF).getBytes(StandardCharsets.UTF_8));
        for (ByteBuffer buffer : elements) {
            result.put(buffer.duplicate());
        }

        result.flip();
        return result;
    }

    public static <T> ByteBuffer streamEntries(List<T> entries,
            Function<T, String> idExtractor,
            Function<T, List<String>> fieldsExtractor) {
        if (entries == null || entries.isEmpty()) {
            return encode("*0" + ProtocolConstants.CRLF);
        }

        StringBuilder response = new StringBuilder();
        response.append("*").append(entries.size()).append(ProtocolConstants.CRLF);

        for (T entry : entries) {
            String id = idExtractor.apply(entry);
            List<String> fields = fieldsExtractor.apply(entry);

            response.append("*2").append(ProtocolConstants.CRLF);
            response.append(bufferToString(bulkString(id)));
            response.append("*").append(fields.size()).append(ProtocolConstants.CRLF);

            for (String field : fields) {
                response.append(bufferToString(bulkString(field)));
            }
        }

        return encode(response.toString());
    }

    // Replication responses
    public static ByteBuffer fullResyncResponse(String replId, long offset) {
        return encode("+FULLRESYNC " + replId + " " + offset + ProtocolConstants.CRLF);
    }

    public static ByteBuffer continueResyncResponse() {
        return encode("+CONTINUE" + ProtocolConstants.CRLF);
    }

    public static ByteBuffer rdbFilePayload(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("RDB file cannot be null");
        }

        byte[] header = ("$" + data.length + ProtocolConstants.CRLF).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(header.length + data.length);
        buffer.put(header);
        buffer.put(data);
        buffer.flip();

        return buffer;
    }

    public static <K, V> ByteBuffer map(Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return encode("*0" + ProtocolConstants.CRLF);
        }

        // Each entry becomes [bulk key, bulk value], so total array size = map.size() *
        // 2
        int totalElements = map.size() * 2;
        StringBuilder response = new StringBuilder();
        response.append("*").append(totalElements).append(ProtocolConstants.CRLF);

        for (Map.Entry<K, V> entry : map.entrySet()) {
            response.append(bufferToString(bulkString(String.valueOf(entry.getKey()))));
            response.append(bufferToString(bulkString(String.valueOf(entry.getValue()))));
        }

        return encode(response.toString());
    }

    // Utility methods
    public static ByteBuffer encode(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String bufferToString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int calculateTotalSize(List<ByteBuffer> elements) {
        int size = ("*" + elements.size() + ProtocolConstants.CRLF).length();
        for (ByteBuffer buffer : elements) {
            size += buffer.remaining();
        }
        return size;
    }
}
