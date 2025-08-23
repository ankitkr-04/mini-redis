package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import blocking.ListBlockingManager;
import blocking.StreamBlockingManager;
import blocking.TimeoutScheduler;
import commands.CommandRegistry;
import common.Constants;
import server.handler.ClientHandler;
import server.protocol.CommandDispatcher;
import storage.InMemoryStorage;
import storage.interfaces.StorageEngine;

public final class Main {
    private final int port;
    private final StorageEngine storage;
    private final CommandDispatcher dispatcher;
    private final ListBlockingManager listBlockingManager;
    private final StreamBlockingManager streamBlockingManager;
    private final TimeoutScheduler timeoutScheduler;

    public Main(int port) {
        this.port = port;
        this.storage = new InMemoryStorage();
        this.listBlockingManager = new ListBlockingManager(storage);
        this.streamBlockingManager = new StreamBlockingManager(storage);
        this.timeoutScheduler = new TimeoutScheduler(listBlockingManager, streamBlockingManager);
        this.dispatcher =
                new CommandDispatcher(
                        CommandRegistry.createDefault(listBlockingManager, streamBlockingManager),
                        storage);
    }

    public static void main(String[] args) {
        new Main(Constants.DEFAULT_PORT).start();
    }

    public void start() {
        System.out.println("Starting KV Server on port " + port + "...");

        // Start background services
        timeoutScheduler.start();

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
                Selector selector = Selector.open()) {

            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server ready and listening on port " + port);

            while (true) {
                selector.select();
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (key.isAcceptable()) {
                        ClientHandler.acceptConnection(key, selector);
                    } else if (key.isReadable()) {
                        ClientHandler.handleClient(key, dispatcher);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
