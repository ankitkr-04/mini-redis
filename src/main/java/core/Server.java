package core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import config.CommandLineParser;
import config.ServerConfig;
import server.handler.ClientHandler;

public final class Server {
    private final int port;
    private final ServerContext context;

    public Server(int port) {
        this.port = port;
        this.context = new ServerContext();
    }

    public static void main(String[] args) {
        // port argument can be passed, --port 6380
        var options = CommandLineParser.parse(args);
        int port = CommandLineParser.getIntOption(options, "port", ServerConfig.DEFAULT_PORT);
        new Server(port).start();
    }

    public void start() {
        System.out.println("Starting Redis Server on port " + port + "...");

        context.start();

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
                        ClientHandler.handleClient(key, context.getCommandDispatcher());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            context.shutdown();
        }
    }
}
