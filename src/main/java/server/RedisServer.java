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
 */
public final class RedisServer {
    private static final Logger log = LoggerFactory.getLogger(RedisServer.class);

    private final ServerConfiguration config;
    private final ServerContext context;

    public RedisServer(ServerConfiguration config) {
        this.config = config;
        this.context = new ServerContext(config);
    }

    public static void main(String[] args) {
        var parseResult = ConfigurationParser.parse(args);

        if (!parseResult.errors().isEmpty()) {
            parseResult.errors().forEach((_, error) -> System.err.println("Config error: " + error));
            System.exit(1);
        }

        var serverConfig = ServerConfiguration.from(parseResult.options());
        new RedisServer(serverConfig).start();
    }

    public void start() {
        log.info("Starting Redis Server on port {}...", config.port());

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
                Selector selector = Selector.open()) {

            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(config.bindAddress(), config.port()));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            context.start(selector);
            log.info("Server ready and listening on {}:{}", config.bindAddress(), config.port());

            runEventLoop(selector);

        } catch (IOException e) {
            log.error("Server error: {}", e.getMessage(), e);
        } finally {
            context.shutdown();
        }
    }

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
                    log.warn("Error handling key: {}", e.getMessage());
                    key.cancel();
                    if (key.channel() != null) {
                        key.channel().close();
                    }
                }
            }
        }
    }

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