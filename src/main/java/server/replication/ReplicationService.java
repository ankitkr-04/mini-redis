package server.replication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ProtocolConstants;
import protocol.ResponseBuilder;

public class ReplicationService {
    private static final Logger log = LoggerFactory.getLogger(ReplicationService.class);

    public void sendResponse(SocketChannel clientChannel, ByteBuffer responseBuffer) throws IOException {
        writeComplete(clientChannel, responseBuffer);

        // Safely extract bytes without assuming backing array
        int len = responseBuffer.limit();
        byte[] data = new byte[len];
        responseBuffer.rewind(); // reset to start
        responseBuffer.get(data); // copy all bytes

        log.trace("Sent response to {}: {}", getChannelInfo(clientChannel), new String(data).trim());
        responseBuffer.rewind(); // rewind again so caller can reuse if needed
    }

    public void sendResponse(SocketChannel clientChannel, String response) throws IOException {
        ByteBuffer responseBuffer = ResponseBuilder.encode(response);
        writeComplete(clientChannel, responseBuffer);
        log.trace("Sent response to {}: {}", getChannelInfo(clientChannel), response.trim());
    }

    public void sendCommand(SocketChannel channel, String... args) throws IOException {
        ByteBuffer buffer = ResponseBuilder.array(List.of(args));
        writeComplete(channel, buffer);
        log.trace("Sent command to {}: {}", getChannelInfo(channel), String.join(" ", args));
    }

    public String readResponse(SocketChannel channel, ByteBuffer buffer) throws IOException {
        buffer.clear();
        int bytesRead = channel.read(buffer);
        if (bytesRead == -1) {
            throw new IOException("Channel closed by remote peer");
        }
        buffer.flip();
        return new String(buffer.array(), 0, buffer.limit());
    }

    public void sendEmptyRdbFile(SocketChannel clientChannel) throws IOException {
        byte[] rdbBytes = ProtocolConstants.EMPTY_RDB_BYTES;
        ByteBuffer bulk = ResponseBuilder.rdbFilePayload(rdbBytes);
        writeComplete(clientChannel, bulk);
        log.debug("Sent empty RDB file to replica ({} bytes)", rdbBytes.length);
    }

    private void writeComplete(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private String getChannelInfo(SocketChannel channel) {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "unknown";
        }
    }
}
