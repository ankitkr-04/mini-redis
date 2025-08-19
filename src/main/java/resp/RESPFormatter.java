package resp;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RESPFormatter {
    private static final String CRLF = "\r\n";

    public static ByteBuffer simpleString(String message) {
        return encode("+" + message + CRLF);
    }

    public static ByteBuffer error(String message) {
        return encode("-" + message + CRLF);
    }

    public static ByteBuffer integer(int value) {
        return encode(":" + value + CRLF);
    }

    public static ByteBuffer bulkString(String value) {
        if (value == null) {
            return nullBulkString();
        }
        return encode("$" + value.length() + CRLF + value + CRLF);
    }

    public static ByteBuffer nullBulkString() {
        return encode("$-1" + CRLF);
    }

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

    private static ByteBuffer encode(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }
}
