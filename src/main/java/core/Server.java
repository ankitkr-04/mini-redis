package core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

import protocol.parser.CommandLineParser;
import server.ServerOptions;
import server.handler.ClientHandler;
import server.replication.ReplicationClient; // Add import

public final class Server {
    private final int port;
    private final ServerContext context;

    public Server(ServerOptions options) {
        this.port = options.port();
        this.context = new ServerContext(options);
    }

    public static void main(String[] args) {
        var parseResult = CommandLineParser.parse(args);
        if (!parseResult.getErrors().isEmpty()) {
            parseResult.getErrors().forEach((_, error) -> System.err.println(error));
            System.exit(1);
        }
        var serverOptions = ServerOptions.from(parseResult.getOptions());
        new Server(serverOptions).start();
    }

    public void start() {
        System.out.println("Starting Redis Server on port " + port + "...");

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
                Selector selector = Selector.open()) {

            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            context.start(selector);

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
                        Object attachment = key.attachment();
                        if (attachment instanceof ReplicationClient) {
                            ((ReplicationClient) attachment).handleKey(key);
                        } else {
                            ClientHandler.handleClient(key, context.getCommandDispatcher());
                        }
                    } else if (key.isConnectable()) {
                        Object attachment = key.attachment();
                        if (attachment instanceof ReplicationClient) {
                            ((ReplicationClient) attachment).handleKey(key);
                        }
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
