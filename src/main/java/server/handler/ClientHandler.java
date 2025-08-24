package server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import config.ServerConfig;
import protocol.CommandDispatcher;
import protocol.parser.ProtocolParser;


public class ClientHandler {
    private ClientHandler() {} // Utility class

    public static void acceptConnection(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ,
                    ByteBuffer.allocate(ServerConfig.BUFFER_SIZE));
            System.out.println("Client connected: " + clientChannel.getRemoteAddress());
        }
    }

    public static void handleClient(SelectionKey key, CommandDispatcher dispatcher)
            throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead > 0) {
            buffer.flip();

            String[] commands = ProtocolParser.parse(buffer);
            ByteBuffer response = dispatcher.dispatch(commands, clientChannel);

            if (response != null) {
                while (response.hasRemaining()) {
                    clientChannel.write(response);
                }
            }

            buffer.clear();

        } else if (bytesRead == -1) {
            System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
            key.cancel();
            clientChannel.close();
        }
    }
}
