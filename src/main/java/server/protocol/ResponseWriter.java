package server.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

public final class ResponseWriter {
    private static final String CRLF = "\r\n";

    private ResponseWriter() {}

    public static ByteBuffer simpleString(String message) {
        return encode("+" + message + CRLF);
    }

    public static ByteBuffer error(String message) {
        return encode("-ERR " + message + CRLF);
    }

    public static ByteBuffer integer(int value) {
        return encode(":" + value + CRLF);
    }

    public static ByteBuffer bulkString(String value) {
        if (value == null) {
            return encode("$-1" + CRLF);
        }
        return encode("$" + value.length() + CRLF + value + CRLF);
    }

    // Original method for backward compatibility
    public static ByteBuffer array(List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode("*0" + CRLF);
        }

        var response = new StringBuilder();
        response.append("*").append(elements.size()).append(CRLF);

        for (String element : elements) {
            if (element == null) {
                response.append("$-1").append(CRLF);
            } else {
                response.append("$").append(element.length()).append(CRLF).append(element)
                        .append(CRLF);
            }
        }

        return encode(response.toString());
    }

    // Method to handle ByteBuffer arrays with different name to avoid erasure conflicts
    public static ByteBuffer arrayOfBuffers(List<ByteBuffer> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode("*0" + CRLF);
        }

        var response = new StringBuilder();
        response.append("*").append(elements.size()).append(CRLF);

        for (ByteBuffer element : elements) {
            if (element == null) {
                response.append("$-1").append(CRLF);
            } else {
                // Convert ByteBuffer back to string to get the actual content
                String content = StandardCharsets.UTF_8.decode(element.duplicate()).toString();
                response.append(content);
            }
        }

        return encode(response.toString());
    }


    public static ByteBuffer arrayOfBuffers(ByteBuffer... elements) {
        return arrayOfBuffers(List.of(elements));
    }


    public static <T> ByteBuffer arrayWith(List<T> elements, Function<T, String> mapper) {
        if (elements == null || elements.isEmpty()) {
            return encode("*0" + CRLF);
        }

        var response = new StringBuilder();
        response.append("*").append(elements.size()).append(CRLF);

        for (T element : elements) {
            if (element == null) {
                response.append("$-1").append(CRLF);
            } else {
                String stringValue = mapper.apply(element);
                response.append("$").append(stringValue.length()).append(CRLF).append(stringValue)
                        .append(CRLF);
            }
        }

        return encode(response.toString());
    }

    // Helper method to convert ByteBuffer to its string content
    public static String bufferToString(ByteBuffer buffer) {
        if (buffer == null)
            return null;
        return StandardCharsets.UTF_8.decode(buffer.duplicate()).toString();
    }

    private static ByteBuffer encode(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }
}
