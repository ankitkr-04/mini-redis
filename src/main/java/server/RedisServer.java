package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ConfigurationParser;
import replication.ReplicationClient;

/**
 * Main entry point for the Redis-like server implementation.
 * Handles client connections and manages the server lifecycle.
 * 
 * <p>
 * This server implements the Redis protocol and provides core functionality
 * including command processing, client connection management, and replication
 * support.
 * The server uses a non-blocking I/O model with NIO selectors for high
 * performance.
 * </p>
 * 
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public final class RedisServer {

    /** Logger instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisServer.class);

    /** Exit code used when configuration parsing fails */
    private static final int CONFIG_ERROR_EXIT_CODE = 1;

    private final ServerConfiguration config;
    private final ServerContext context;

    public RedisServer(ServerConfiguration config) {
        this.config = config;
        this.context = new ServerContext(config);
    }

    /**
     * Main entry point for the Redis server application.
     * 
     * @param args command line arguments for server configuration
     */
    public static void main(String[] args) {
        var parseResult = ConfigurationParser.parse(args);

        if (!parseResult.errors().isEmpty()) {
            parseResult.errors().forEach((_, error) -> LOGGER.error("Configuration error: {}", error));
            System.exit(CONFIG_ERROR_EXIT_CODE);
        }

        var serverConfig = ServerConfiguration.from(parseResult.options());
        new RedisServer(serverConfig).start();
    }

    /**
     * Starts the Redis server and begins accepting client connections.
     * This method initializes the server socket, selector, and enters the main
     * event loop.
     */
    public void start() {
        LOGGER.info("Starting Redis Server on port {}...", config.port());

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
                Selector selector = Selector.open()) {

            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(config.bindAddress(), config.port()));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            context.start(selector);
            LOGGER.info("Server ready and listening on {}:{}", config.bindAddress(), config.port());

            runEventLoop(selector);

        } catch (IOException e) {
            LOGGER.error("Server error: {}", e.getMessage(), e);
        } finally {
            context.shutdown();
        }
    }

    /**
     * Main event loop that processes client connections and I/O operations.
     * Uses NIO selector for efficient non-blocking I/O handling.
     * 
     * @param selector the NIO selector for managing channels
     * @throws IOException if an I/O error occurs during event processing
     */
    private void runEventLoop(Selector selector) throws IOException {
        while (true) {
            selector.select();
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                try {
                    handleSelectionKey(key, selector);
                } catch (IOException e) {
                    LOGGER.warn("Error handling key: {}", e.getMessage());
                    key.cancel();
                    if (key.channel() != null) {
                        key.channel().close();
                    }
                }
            }
        }
    }

    /**
     * Handles different types of selection key events (accept, read, connect).
     * 
     * @param key      the selection key to handle
     * @param selector the NIO selector
     * @throws IOException if an I/O error occurs during key handling
     */
    private void handleSelectionKey(SelectionKey key, Selector selector) throws IOException {
        if (key.isAcceptable()) {
            ClientConnectionHandler.acceptNewConnection(key, selector, context);
        } else if (key.isReadable()) {
            Object attachment = key.attachment();
            if (attachment instanceof ReplicationClient) {
                ((ReplicationClient) attachment).handleKey(key);
            } else {
                ClientConnectionHandler.handleClientRequest(key, context.getCommandDispatcher(), context);
            }
        } else if (key.isConnectable()) {
            Object attachment = key.attachment();
            if (attachment instanceof ReplicationClient) {
                ((ReplicationClient) attachment).handleKey(key);
            }
        }
    }
}