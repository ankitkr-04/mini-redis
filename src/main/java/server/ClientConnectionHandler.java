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

    public static void acceptNewConnection(SelectionKey key, Selector selector, ServerContext serverContext)
            throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
            log.info("Client connected: {}", clientChannel.getRemoteAddress());

            // Record Redis Enterprise compatible connection metrics
            var metricsCollector = serverContext.getMetricsCollector();
            metricsCollector.recordClientConnection();
        }
    }

    public static void handleClientRequest(SelectionKey key, CommandDispatcher dispatcher, ServerContext serverContext)
            throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead > 0) {
            // Record network input metrics
            serverContext.getMetricsCollector().recordNetworkInput(bytesRead);

            buffer.flip();
            String[] commands = ProtocolParser.parse(buffer);
            ByteBuffer response = dispatcher.dispatch(commands, clientChannel);

            if (response != null) {
                int responseSize = response.remaining();
                writeCompleteResponse(clientChannel, response);

                // Record network output metrics
                serverContext.getMetricsCollector().recordNetworkOutput(responseSize);
            }
            buffer.clear();

        } else if (bytesRead == -1) {
            log.info("Client disconnected: {}", clientChannel.getRemoteAddress());

            // Record Redis Enterprise compatible disconnection metrics
            var metricsCollector = serverContext.getMetricsCollector();
            metricsCollector.recordClientDisconnection();

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