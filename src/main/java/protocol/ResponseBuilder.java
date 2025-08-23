package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import config.RedisConstants;

public final class ResponseBuilder {

    private ResponseBuilder() {}

    public static ByteBuffer simpleString(String message) {
        return encode("+" + message + RedisConstants.CRLF);
    }

    public static ByteBuffer error(String message) {
        return encode("-ERR " + message + RedisConstants.CRLF);
    }

    public static ByteBuffer integer(int value) {
        return encode(":" + value + RedisConstants.CRLF);
    }

    public static ByteBuffer integer(long value) {
        return encode(":" + value + RedisConstants.CRLF);
    }

    public static ByteBuffer bulkString(String value) {
        if (value == null) {
            return encode(RedisConstants.NULL_BULK_STRING);
        }
        return encode("$" + value.length() + RedisConstants.CRLF + value + RedisConstants.CRLF);
    }

    public static ByteBuffer array(List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode(RedisConstants.EMPTY_ARRAY);
        }

        StringBuilder response = new StringBuilder();
        response.append("*").append(elements.size()).append(RedisConstants.CRLF);

        for (String str : elements) {
            if (str == null) {
                response.append(RedisConstants.NULL_BULK_STRING);
            } else {
                response.append("$").append(str.length())
                        .append(RedisConstants.CRLF)
                        .append(str)
                        .append(RedisConstants.CRLF);
            }
        }

        return encode(response.toString());
    }

    public static ByteBuffer arrayOfBuffers(List<ByteBuffer> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode(RedisConstants.EMPTY_ARRAY);
        }

        int totalSize = "*".length() + Integer.toString(elements.size()).length()
                + RedisConstants.CRLF.length();
        for (ByteBuffer buf : elements) {
            totalSize += buf.remaining();
        }

        ByteBuffer result = ByteBuffer.allocate(totalSize);
        result.put(("*" + elements.size() + RedisConstants.CRLF).getBytes(StandardCharsets.UTF_8));
        for (ByteBuffer buf : elements) {
            result.put(buf.duplicate());
        }
        result.flip();
        return result;
    }

    public static <T> ByteBuffer arrayWith(List<T> elements, Function<T, String> mapper) {
        if (elements == null || elements.isEmpty()) {
            return encode(RedisConstants.EMPTY_ARRAY);
        }

        StringBuilder response = new StringBuilder();
        response.append("*").append(elements.size()).append(RedisConstants.CRLF);

        for (T element : elements) {
            if (element == null) {
                response.append(RedisConstants.NULL_BULK_STRING);
            } else {
                String stringValue = mapper.apply(element);
                response.append("$").append(stringValue.length())
                        .append(RedisConstants.CRLF)
                        .append(stringValue)
                        .append(RedisConstants.CRLF);
            }
        }

        return encode(response.toString());
    }

    // Stream-specific response building
    public static <T> ByteBuffer streamEntries(List<T> entries,
            Function<T, String> idExtractor,
            Function<T, List<String>> fieldsExtractor) {
        StringBuilder response = new StringBuilder();
        response.append("*").append(entries.size()).append(RedisConstants.CRLF);

        for (T entry : entries) {
            String id = idExtractor.apply(entry);
            List<String> fields = fieldsExtractor.apply(entry);

            // Each entry is an array of [id, [field1, value1, field2, value2, ...]]
            response.append("*2").append(RedisConstants.CRLF);

            // Add ID
            response.append("$").append(id.length())
                    .append(RedisConstants.CRLF)
                    .append(id)
                    .append(RedisConstants.CRLF);

            // Add fields array
            response.append("*").append(fields.size()).append(RedisConstants.CRLF);
            for (String field : fields) {
                response.append("$").append(field.length())
                        .append(RedisConstants.CRLF)
                        .append(field)
                        .append(RedisConstants.CRLF);
            }
        }

        return encode(response.toString());
    }

    public static ByteBuffer encode(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }
}
