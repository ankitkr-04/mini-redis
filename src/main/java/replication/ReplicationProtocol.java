package replication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protocol.ResponseBuilder;

public final class ReplicationProtocol {
    private static final Logger log = LoggerFactory.getLogger(ReplicationProtocol.class);

    public enum HandshakeState {
        INITIAL, PING_SENT, REPLCONF_PORT_SENT, REPLCONF_CAPA_SENT,
        PSYNC_SENT, RDB_RECEIVING, ACTIVE
    }

    private ReplicationProtocol() {
    } // Utility class

    public static void sendCommand(SocketChannel channel, String... args) throws IOException {
        ByteBuffer buffer = ResponseBuilder.array(List.of(args));
        writeComplete(channel, buffer);
        log.trace("Sent command to {}: {}", getChannelInfo(channel), String.join(" ", args));
    }

    public static void sendResponse(SocketChannel channel, ByteBuffer response) throws IOException {
        writeComplete(channel, response);
        log.trace("Sent response to {}: {} bytes", getChannelInfo(channel), response.remaining());
    }

    public static void sendEmptyRdbFile(SocketChannel channel) throws IOException {
        byte[] emptyRdb = {
                'R', 'E', 'D', 'I', 'S', '0', '0', '1', '1', // magic + version
                (byte) 0xFF, // EOF marker
                0, 0, 0, 0, 0, 0, 0, 0 // checksum (8 bytes of zero)
        };
        ByteBuffer rdbPayload = ResponseBuilder.rdbFilePayload(emptyRdb);
        writeComplete(channel, rdbPayload);
        log.debug("Sent empty RDB file to replica ({} bytes)", emptyRdb.length);
    }

    public static long calculateCommandSize(String[] commandArgs) {
        long size = 1 + String.valueOf(commandArgs.length).length() + 2; // *<count>\r\n
        for (String arg : commandArgs) {
            size += 1 + String.valueOf(arg.length()).length() + 2 + arg.length() + 2; // $<len>\r\n<arg>\r\n
        }
        return size;
    }

    private static void writeComplete(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private static String getChannelInfo(SocketChannel channel) {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "unknown";
        }
    }
}