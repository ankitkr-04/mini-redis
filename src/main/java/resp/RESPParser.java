package resp;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Parser for Redis Serialization Protocol (RESP) messages. Handles RESP arrays and simple strings
 * according to RESP specification.
 */
public final class RESPParser {

    // RESP protocol constants
    private static final char ARRAY_PREFIX = '*';
    private static final char BULK_STRING_PREFIX = '$';
    private static final String CRLF_DELIMITER = "\r\n";
    private static final String[] EMPTY_RESULT = new String[0];
    private static final int MINIMUM_ARRAY_LINES = 2; // At least "*count\r\n" + one element

    /**
     * Parses a RESP message from a ByteBuffer into a string array.
     * 
     * @param buffer The ByteBuffer containing RESP-formatted data
     * @return Array of parsed strings, empty array if parsing fails
     */
    public static String[] parse(ByteBuffer buffer) {
        String message = extractStringFromBuffer(buffer);

        if (isEmptyMessage(message)) {
            return EMPTY_RESULT;
        }

        return isRespArray(message) ? parseRespArray(message) : parseSingleCommand(message);
    }

    /**
     * Extracts string content from ByteBuffer.
     */
    private static String extractStringFromBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Checks if the message is empty or null.
     */
    private static boolean isEmptyMessage(String message) {
        return message == null || message.isEmpty();
    }

    /**
     * Determines if the message is a RESP array format.
     */
    private static boolean isRespArray(String message) {
        return message.charAt(0) == ARRAY_PREFIX;
    }

    /**
     * Parses a RESP array message into string array. Format: *<count>\r\n$<len>\r\n<data>\r\n...
     */
    private static String[] parseRespArray(String message) {
        String[] lines = message.split(CRLF_DELIMITER);

        if (hasInsufficientLines(lines)) {
            return EMPTY_RESULT;
        }

        int arraySize = parseArraySize(lines[0]);
        if (arraySize < 0) {
            return EMPTY_RESULT; // Invalid array size
        }

        return extractArrayElements(lines, arraySize);
    }

    /**
     * Checks if there are enough lines for a valid RESP array.
     */
    private static boolean hasInsufficientLines(String[] lines) {
        return lines.length < MINIMUM_ARRAY_LINES;
    }

    /**
     * Parses the array size from the first line (e.g., "*3" -> 3).
     */
    private static int parseArraySize(String firstLine) {
        try {
            return Integer.parseInt(firstLine.substring(1));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return -1; // Invalid format
        }
    }

    /**
     * Extracts array elements from RESP bulk string format.
     */
    private static String[] extractArrayElements(String[] lines, int expectedSize) {
        String[] result = new String[expectedSize];
        int currentIndex = 0;
        int lineIndex = 1; // Start after the array size line

        while (lineIndex < lines.length && currentIndex < expectedSize) {
            if (isBulkStringHeader(lines[lineIndex])) {
                String element = extractBulkStringContent(lines, lineIndex);
                if (element != null) {
                    result[currentIndex++] = element;
                    lineIndex += 2; // Skip both header and content lines
                } else {
                    break; // Malformed RESP, stop parsing
                }
            } else {
                lineIndex++; // Skip unexpected lines
            }
        }

        return result;
    }

    /**
     * Checks if a line is a bulk string header (starts with '$').
     */
    private static boolean isBulkStringHeader(String line) {
        return line.startsWith(String.valueOf(BULK_STRING_PREFIX));
    }

    /**
     * Extracts content from a bulk string, handling boundary checks.
     */
    private static String extractBulkStringContent(String[] lines, int headerIndex) {
        int contentIndex = headerIndex + 1;

        if (isContentLineAvailable(lines, contentIndex)) {
            return lines[contentIndex];
        }

        return null; // Malformed RESP
    }

    /**
     * Verifies that the content line exists and is within bounds.
     */
    private static boolean isContentLineAvailable(String[] lines, int contentIndex) {
        return contentIndex < lines.length;
    }

    /**
     * Handles simple string commands (non-array format).
     */
    private static String[] parseSingleCommand(String message) {
        return new String[] {message.trim()};
    }
}
