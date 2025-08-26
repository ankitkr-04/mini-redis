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

/**
 * Handles client connections and communication for the Redis server.
 * 
 * <p>
 * This class manages the lifecycle of client connections including
 * accepting new connections, reading client requests, processing commands,
 * and sending responses back to clients. It uses non-blocking I/O operations
 * for efficient handling of multiple concurrent clients.
 * </p>
 * 
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public final class ClientConnectionHandler {

    /** Logger instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionHandler.class);

    /** Buffer size for client communication in bytes */
    private static final int BUFFER_SIZE = 1024;

    /** Indicates end of stream when reading from client */
    private static final int END_OF_STREAM = -1;

    private ClientConnectionHandler() {
        // Utility class - prevent instantiation
    }

    /**
     * Accepts a new client connection and registers it with the selector.
     * 
     * @param key           the selection key for the server socket
     * @param selector      the NIO selector for managing channels
     * @param serverContext the server context containing shared resources
     * @throws IOException if an I/O error occurs during connection acceptance
     */
    public static void acceptNewConnection(SelectionKey key, Selector selector, ServerContext serverContext)
            throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
            LOGGER.info("Client connected: {}", clientChannel.getRemoteAddress());

            // Record Redis Enterprise compatible connection metrics
            var metricsCollector = serverContext.getMetricsCollector();
            metricsCollector.recordClientConnection();
        }
    }

    /**
     * Handles incoming client requests and processes commands.
     * 
     * @param key           the selection key for the client socket
     * @param dispatcher    the command dispatcher for processing commands
     * @param serverContext the server context containing shared resources
     * @throws IOException if an I/O error occurs during request handling
     */
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

        } else if (bytesRead == END_OF_STREAM) {
            LOGGER.info("Client disconnected: {}", clientChannel.getRemoteAddress());

            // Record Redis Enterprise compatible disconnection metrics
            var metricsCollector = serverContext.getMetricsCollector();
            metricsCollector.recordClientDisconnection();

            key.cancel();
            clientChannel.close();
        }
    }

    /**
     * Writes a complete response to the client channel.
     * Ensures all bytes are written even if the write operation is partial.
     * 
     * @param channel  the client socket channel
     * @param response the response buffer to write
     * @throws IOException if an I/O error occurs during writing
     */
    private static void writeCompleteResponse(SocketChannel channel, ByteBuffer response) throws IOException {
        while (response.hasRemaining()) {
            channel.write(response);
        }
    }
}