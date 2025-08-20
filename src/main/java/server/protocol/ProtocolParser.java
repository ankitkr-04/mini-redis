package server.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ProtocolParser {
    private static final String CRLF = "\r\n";
    private static final String[] EMPTY_RESULT = new String[0];

    private ProtocolParser() {} // Utility class

    public static String[] parse(ByteBuffer buffer) {
        String message = extractString(buffer);

        if (message == null || message.isEmpty()) {
            return EMPTY_RESULT;
        }

        return message.charAt(0) == '*' ? parseArray(message) : new String[] {message.trim()};
    }

    private static String extractString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String[] parseArray(String message) {
        String[] lines = message.split(CRLF);

        if (lines.length < 2) {
            return EMPTY_RESULT;
        }

        try {
            int arraySize = Integer.parseInt(lines[0].substring(1));
            if (arraySize < 0)
                return EMPTY_RESULT;

            String[] result = new String[arraySize];
            int resultIndex = 0;
            int lineIndex = 1;

            while (lineIndex < lines.length && resultIndex < arraySize) {
                if (lines[lineIndex].startsWith("$")) {
                    int contentIndex = lineIndex + 1;
                    if (contentIndex < lines.length) {
                        result[resultIndex++] = lines[contentIndex];
                        lineIndex += 2; // Skip length line and content line
                    } else {
                        break;
                    }
                } else {
                    lineIndex++;
                }
            }

            return result;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return EMPTY_RESULT;
        }
    }
}
