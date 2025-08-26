package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses RESP (REdis Serialization Protocol) messages from a ByteBuffer.
 * Supports parsing RESP arrays, bulk strings, and simple strings.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class ProtocolParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolParser.class);

    private static final String[] EMPTY_RESULT = new String[0];
    private static final String CRLF = "\r\n";
    private static final byte ASTERISK = (byte) '*';
    private static final byte DOLLAR = (byte) '$';
    private static final byte PLUS = (byte) '+';
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';

    private ProtocolParser() {
        // Utility class
    }

    /**
     * Parses multiple RESP array commands from the buffer.
     * 
     * @param buffer ByteBuffer containing RESP data
     * @return List of String arrays, each representing a command
     */
    public static List<String[]> parseRespArrays(ByteBuffer buffer) {
        List<String[]> commands = new ArrayList<>();
        while (buffer.hasRemaining()) {
            int initialPosition = buffer.position();
            String[] command = parseRespArray(buffer);
            if (command == null) {
                buffer.position(initialPosition);
                break;
            }
            commands.add(command);
        }
        return commands;
    }

    /**
     * Parses a RESP Simple String from the buffer.
     * 
     * @param buffer ByteBuffer containing RESP data
     * @return Parsed simple string, or null if incomplete
     */
    public static String parseSimpleString(ByteBuffer buffer) {
        int initialPosition = buffer.position();
        if (!buffer.hasRemaining() || buffer.get() != PLUS) {
            buffer.position(initialPosition);
            return null;
        }

        StringBuilder result = new StringBuilder();
        while (buffer.hasRemaining()) {
            byte currentByte = buffer.get();
            if (currentByte == CR) {
                if (buffer.hasRemaining() && buffer.get() == LF) {
                    return result.toString();
                }
                buffer.position(initialPosition);
                return null;
            }
            result.append((char) currentByte);
        }
        buffer.position(initialPosition);
        return null;
    }

    /**
     * Parses a RESP message from the buffer.
     * 
     * @param buffer ByteBuffer containing RESP data
     * @return Array of strings representing the command and arguments
     */
    public static String[] parse(ByteBuffer buffer) {
        String message = extractString(buffer);
        if (message == null || message.isEmpty()) {
            return EMPTY_RESULT;
        }
        return message.charAt(0) == '*' ? parseArray(message) : new String[] { message.trim() };
    }

    /**
     * Parses a single RESP array from the buffer.
     * 
     * @param buffer ByteBuffer containing RESP data
     * @return Array of strings representing the command and arguments, or null if
     *         incomplete
     */
    private static String[] parseRespArray(ByteBuffer buffer) {
        if (!buffer.hasRemaining() || buffer.get() != ASTERISK) {
            return null;
        }

        Long arraySize = parseNumber(buffer);
        if (arraySize == null || arraySize < 0)
            return null;

        String[] args = new String[arraySize.intValue()];
        for (int i = 0; i < arraySize; i++) {
            String arg = parseBulkString(buffer);
            if (arg == null)
                return null;
            args[i] = arg;
        }
        return args;
    }

    /**
     * Parses a RESP Bulk String from the buffer.
     * 
     * @param buffer ByteBuffer containing RESP data
     * @return Parsed bulk string, or null if incomplete
     */
    private static String parseBulkString(ByteBuffer buffer) {
        if (!buffer.hasRemaining() || buffer.get() != DOLLAR) {
            return null;
        }

        Long length = parseNumber(buffer);
        if (length == null || length < 0)
            return null;

        int bulkLength = length.intValue();
        if (buffer.remaining() < bulkLength + 2)
            return null;

        byte[] data = new byte[bulkLength];
        buffer.get(data);

        if (!buffer.hasRemaining() || buffer.get() != CR ||
                !buffer.hasRemaining() || buffer.get() != LF) {
            return null;
        }

        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Parses a number (terminated by CRLF) from the buffer.
     * 
     * @param buffer ByteBuffer containing RESP data
     * @return Parsed number, or null if incomplete or invalid
     */
    private static Long parseNumber(ByteBuffer buffer) {
        StringBuilder numberBuilder = new StringBuilder();
        while (buffer.hasRemaining()) {
            byte currentByte = buffer.get();
            if (currentByte == CR) {
                if (buffer.hasRemaining() && buffer.get() == LF) {
                    break;
                }
                return null;
            }
            numberBuilder.append((char) currentByte);
        }

        try {
            return Long.parseLong(numberBuilder.toString());
        } catch (NumberFormatException e) {
            LOGGER.debug("Failed to parse number from RESP: {}", numberBuilder, e);
            return null;
        }
    }

    /**
     * Parses a RESP array from a string message.
     * 
     * @param message RESP message as string
     * @return Array of strings representing the command and arguments
     */
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
            LOGGER.debug("Failed to parse RESP array: {}", message, e);
            return EMPTY_RESULT;
        }
    }

    /**
     * Extracts the remaining bytes from the buffer as a UTF-8 string.
     * 
     * @param buffer ByteBuffer containing RESP data
     * @return Extracted string
     */
    private static String extractString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}