package replication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protocol.ResponseBuilder;

/**
 * Provides protocol utilities for master-replica synchronization in Redis-like
 * replication.
 */
public final class ReplicationProtocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationProtocol.class);

    // Protocol constants
    private static final byte[] EMPTY_RDB_FILE = {
            'R', 'E', 'D', 'I', 'S', '0', '0', '1', '1', // magic + version
            (byte) 0xFF, // EOF marker
            0, 0, 0, 0, 0, 0, 0, 0 // checksum (8 bytes of zero)
    };

    private static final int RDB_FILE_LENGTH = EMPTY_RDB_FILE.length;

    /**
     * Represents the handshake state during replication.
     */
    public enum HandshakeState {
        INITIAL, PING_SENT, REPLCONF_PORT_SENT, REPLCONF_CAPA_SENT,
        PSYNC_SENT, RDB_RECEIVING, ACTIVE
    }

    private ReplicationProtocol() {
        // Utility class; prevent instantiation
    }

    /**
     * Sends a command to the given channel using RESP array encoding.
     *
     * @param channel the target socket channel
     * @param args    the command arguments
     * @throws IOException if an I/O error occurs
     */
    public static void sendCommand(SocketChannel channel, String... args) throws IOException {
        ByteBuffer commandBuffer = ResponseBuilder.array(List.of(args));
        writeFully(channel, commandBuffer);
        LOGGER.debug("Sent command to {}: {}", getRemoteAddress(channel), String.join(" ", args));
    }

    /**
     * Sends a pre-built RESP response to the given channel.
     *
     * @param channel  the target socket channel
     * @param response the response buffer
     * @throws IOException if an I/O error occurs
     */
    public static void sendResponse(SocketChannel channel, ByteBuffer response) throws IOException {
        writeFully(channel, response);
        LOGGER.trace("Sent response to {} ({} bytes)", getRemoteAddress(channel), response.remaining());
    }

    /**
     * Sends an empty RDB file payload to the replica.
     *
     * @param channel the target socket channel
     * @throws IOException if an I/O error occurs
     */
    public static void sendEmptyRdbFile(SocketChannel channel) throws IOException {
        ByteBuffer rdbPayload = ResponseBuilder.rdbFilePayload(EMPTY_RDB_FILE);
        writeFully(channel, rdbPayload);
        LOGGER.info("Sent empty RDB file to replica ({} bytes)", RDB_FILE_LENGTH);
    }

    /**
     * Calculates the RESP-encoded size of a command.
     *
     * @param commandArgs the command arguments
     * @return the size in bytes
     */
    public static long calculateCommandSize(String[] commandArgs) {
        long totalSize = 1 + String.valueOf(commandArgs.length).length() + 2; // *<count>\r\n
        for (String argument : commandArgs) {
            totalSize += 1 + String.valueOf(argument.length()).length() + 2 + argument.length() + 2; // $<len>\r\n<arg>\r\n
        }
        return totalSize;
    }

    /**
     * Writes the entire buffer to the channel.
     *
     * @param channel the target socket channel
     * @param buffer  the buffer to write
     * @throws IOException if an I/O error occurs
     */
    private static void writeFully(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    /**
     * Gets the remote address of the channel as a string.
     *
     * @param channel the socket channel
     * @return the remote address string, or "unknown" if unavailable
     */
    private static String getRemoteAddress(SocketChannel channel) {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "unknown";
        }
    }
}