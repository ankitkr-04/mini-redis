package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ProtocolParser {
    private static final String[] EMPTY_RESULT = new String[0];
    private static final String CRLF = "\r\n";

    private ProtocolParser() {
    } // Utility class

    public static List<String[]> parseRespArrays(ByteBuffer buffer) {
        List<String[]> commands = new ArrayList<>();
        while (buffer.hasRemaining()) {
            int start = buffer.position();
            String[] command = parseRespArray(buffer);
            if (command == null) {
                buffer.position(start);
                break;
            }
            commands.add(command);
        }
        return commands;
    }

    public static String parseSimpleString(ByteBuffer buffer) {
        int start = buffer.position();
        if (!buffer.hasRemaining() || buffer.get() != (byte) '+') {
            buffer.position(start);
            return null;
        }

        StringBuilder result = new StringBuilder();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == (byte) '\r') {
                if (buffer.hasRemaining() && buffer.get() == (byte) '\n') {
                    return result.toString();
                }
                buffer.position(start);
                return null;
            }
            result.append((char) b);
        }
        buffer.position(start);
        return null;
    }

    public static String[] parse(ByteBuffer buffer) {
        String message = extractString(buffer);
        if (message == null || message.isEmpty()) {
            return EMPTY_RESULT;
        }

        return message.charAt(0) == '*' ? parseArray(message) : new String[] { message.trim() };
    }

    private static String[] parseRespArray(ByteBuffer buffer) {
        if (!buffer.hasRemaining() || buffer.get() != (byte) '*') {
            return null;
        }

        Long size = parseNumber(buffer);
        if (size == null || size < 0)
            return null;

        String[] args = new String[size.intValue()];
        for (int i = 0; i < size; i++) {
            String arg = parseBulkString(buffer);
            if (arg == null)
                return null;
            args[i] = arg;
        }
        return args;
    }

    private static String parseBulkString(ByteBuffer buffer) {
        if (!buffer.hasRemaining() || buffer.get() != (byte) '$') {
            return null;
        }

        Long length = parseNumber(buffer);
        if (length == null || length < 0)
            return null;

        int len = length.intValue();
        if (buffer.remaining() < len + 2)
            return null;

        byte[] data = new byte[len];
        buffer.get(data);

        if (!buffer.hasRemaining() || buffer.get() != (byte) '\r' ||
                !buffer.hasRemaining() || buffer.get() != (byte) '\n') {
            return null;
        }

        return new String(data, StandardCharsets.UTF_8);
    }

    private static Long parseNumber(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == (byte) '\r') {
                if (buffer.hasRemaining() && buffer.get() == (byte) '\n') {
                    break;
                }
                return null;
            }
            sb.append((char) b);
        }

        try {
            return Long.parseLong(sb.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String[] parseArray(String message) {
        String[] lines = message.split(CRLF);
        if (lines.length < 2)
            return EMPTY_RESULT;

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
                        lineIndex += 2;
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

    private static String extractString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}