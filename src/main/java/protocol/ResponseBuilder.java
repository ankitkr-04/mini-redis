package protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ProtocolConstants;

/**
 * Utility class for building Redis protocol responses.
 * Provides static methods to encode various Redis response types (simple
 * string, error, integer, bulk string, array, map, etc.)
 * according to the RESP protocol.
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public final class ResponseBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBuilder.class);

    // Protocol response prefixes
    private static final String SIMPLE_STRING_PREFIX = "+";
    private static final String ERROR_PREFIX = "-ERR ";
    private static final String INTEGER_PREFIX = ":";
    private static final String BULK_STRING_PREFIX = "$";
    private static final String ARRAY_PREFIX = "*";
    private static final String FULLRESYNC_PREFIX = "+FULLRESYNC ";
    private static final String CONTINUE_PREFIX = "+CONTINUE";
    private static final String NIL_BULK_STRING = "$-1";
    private static final String CRLF = ProtocolConstants.CRLF;

    private ResponseBuilder() {
        // Utility class; prevent instantiation
    }

    /**
     * Encodes a simple string response.
     * 
     * @param message the message to encode
     * @return ByteBuffer containing the encoded response
     */
    public static ByteBuffer simpleString(String message) {
        return encode(SIMPLE_STRING_PREFIX + message + CRLF);
    }

    /**
     * Encodes an error response.
     * 
     * @param message the error message
     * @return ByteBuffer containing the encoded error
     */
    public static ByteBuffer error(String message) {
        return encode(ERROR_PREFIX + message + CRLF);
    }

    /**
     * Encodes an integer response.
     * 
     * @param value the integer value
     * @return ByteBuffer containing the encoded integer
     */
    public static ByteBuffer integer(long value) {
        return encode(INTEGER_PREFIX + value + CRLF);
    }

    /**
     * Encodes a bulk string response.
     * 
     * @param value the string value, or null for nil
     * @return ByteBuffer containing the encoded bulk string
     */
    public static ByteBuffer bulkString(String value) {
        if (value == null) {
            return encode(NIL_BULK_STRING + CRLF);
        }
        return encode(BULK_STRING_PREFIX + value.length() + CRLF + value + CRLF);
    }

    /**
     * Encodes an array of strings as a RESP array.
     * 
     * @param elements list of string elements
     * @return ByteBuffer containing the encoded array
     */
    public static ByteBuffer array(List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return encode(ARRAY_PREFIX + "0" + CRLF);
        }

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append(ARRAY_PREFIX).append(elements.size()).append(CRLF);

        for (String element : elements) {
            responseBuilder.append(bufferToString(bulkString(element)));
        }

        return encode(responseBuilder.toString());
    }

    /**
     * Encodes an array of ByteBuffers as a RESP array.
     * 
     * @param buffers list of ByteBuffer elements
     * @return ByteBuffer containing the encoded array
     */
    public static ByteBuffer arrayOfBuffers(List<ByteBuffer> buffers) {
        if (buffers == null || buffers.isEmpty()) {
            return encode(ARRAY_PREFIX + "0" + CRLF);
        }

        int totalSize = calculateTotalSize(buffers);
        ByteBuffer resultBuffer = ByteBuffer.allocate(totalSize);

        resultBuffer.put((ARRAY_PREFIX + buffers.size() + CRLF).getBytes(StandardCharsets.UTF_8));
        for (ByteBuffer buffer : buffers) {
            resultBuffer.put(buffer.duplicate());
        }

        resultBuffer.flip();
        return resultBuffer;
    }

    /**
     * Encodes a list of stream entries as a RESP array.
     * 
     * @param entries         list of entries
     * @param idExtractor     function to extract entry id
     * @param fieldsExtractor function to extract entry fields
     * @param <T>             entry type
     * @return ByteBuffer containing the encoded stream entries
     */
    public static <T> ByteBuffer streamEntries(List<T> entries,
            Function<T, String> idExtractor,
            Function<T, List<String>> fieldsExtractor) {
        if (entries == null || entries.isEmpty()) {
            return encode(ARRAY_PREFIX + "0" + CRLF);
        }

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append(ARRAY_PREFIX).append(entries.size()).append(CRLF);

        for (T entry : entries) {
            String id = idExtractor.apply(entry);
            List<String> fields = fieldsExtractor.apply(entry);

            responseBuilder.append(ARRAY_PREFIX).append("2").append(CRLF);
            responseBuilder.append(bufferToString(bulkString(id)));
            responseBuilder.append(ARRAY_PREFIX).append(fields.size()).append(CRLF);

            for (String field : fields) {
                responseBuilder.append(bufferToString(bulkString(field)));
            }
        }

        return encode(responseBuilder.toString());
    }

    /**
     * Encodes a full resync response for replication.
     * 
     * @param replId replication id
     * @param offset replication offset
     * @return ByteBuffer containing the encoded response
     */
    public static ByteBuffer fullResyncResponse(String replId, long offset) {
        return encode(FULLRESYNC_PREFIX + replId + " " + offset + CRLF);
    }

    /**
     * Encodes a continue resync response for replication.
     * 
     * @return ByteBuffer containing the encoded response
     */
    public static ByteBuffer continueResyncResponse() {
        return encode(CONTINUE_PREFIX + CRLF);
    }

    /**
     * Encodes an RDB file payload as a RESP bulk string.
     * 
     * @param data the RDB file data
     * @return ByteBuffer containing the encoded payload
     * @throws IllegalArgumentException if data is null
     */
    public static ByteBuffer rdbFilePayload(byte[] data) {
        if (data == null) {
            LOGGER.error("RDB file cannot be null");
            throw new IllegalArgumentException("RDB file cannot be null");
        }

        byte[] header = (BULK_STRING_PREFIX + data.length + CRLF).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(header.length + data.length);
        buffer.put(header);
        buffer.put(data);
        buffer.flip();

        return buffer;
    }

    /**
     * Encodes a map as a RESP array of bulk strings (key, value pairs).
     * 
     * @param map the map to encode
     * @param <K> key type
     * @param <V> value type
     * @return ByteBuffer containing the encoded map
     */
    public static <K, V> ByteBuffer map(Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return encode(ARRAY_PREFIX + "0" + CRLF);
        }

        int totalElements = map.size() * 2;
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append(ARRAY_PREFIX).append(totalElements).append(CRLF);

        for (Map.Entry<K, V> entry : map.entrySet()) {
            responseBuilder.append(bufferToString(bulkString(String.valueOf(entry.getKey()))));
            responseBuilder.append(bufferToString(bulkString(String.valueOf(entry.getValue()))));
        }

        return encode(responseBuilder.toString());
    }

    /**
     * Encodes a string as a ByteBuffer using UTF-8.
     * 
     * @param text the string to encode
     * @return ByteBuffer containing the encoded string
     */
    public static ByteBuffer encode(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a ByteBuffer to a String using UTF-8.
     * 
     * @param buffer the ByteBuffer to convert
     * @return String representation of the buffer
     */
    private static String bufferToString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Calculates the total size needed for a RESP array of ByteBuffers.
     * 
     * @param buffers list of ByteBuffers
     * @return total size in bytes
     */
    private static int calculateTotalSize(List<ByteBuffer> buffers) {
        int size = (ARRAY_PREFIX + buffers.size() + CRLF).length();
        for (ByteBuffer buffer : buffers) {
            size += buffer.remaining();
        }
        return size;
    }
}
