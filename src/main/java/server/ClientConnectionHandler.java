package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protocol.CommandDispatcher;
import protocol.ProtocolParser;

public final class ClientConnectionHandler {
    private static final Logger log = LoggerFactory.getLogger(ClientConnectionHandler.class);
    private static final int BUFFER_SIZE = 1024;

    private ClientConnectionHandler() {
    } // Utility class

    public static void acceptNewConnection(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
            log.info("Client connected: {}", clientChannel.getRemoteAddress());
        }
    }

    public static void handleClientRequest(SelectionKey key, CommandDispatcher dispatcher) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead > 0) {
            buffer.flip();
            String[] commands = ProtocolParser.parse(buffer);
            ByteBuffer response = dispatcher.dispatch(commands, clientChannel);

            if (response != null) {
                writeCompleteResponse(clientChannel, response);
            }
            buffer.clear();

        } else if (bytesRead == -1) {
            log.info("Client disconnected: {}", clientChannel.getRemoteAddress());
            key.cancel();
            clientChannel.close();
        }
    }

    private static void writeCompleteResponse(SocketChannel channel, ByteBuffer response) throws IOException {
        while (response.hasRemaining()) {
            channel.write(response);
        }
    }
}