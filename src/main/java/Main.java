import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import resp.RESPParser;
import store.DataStore;

public class Main {

  private static final int PORT = 6379;
  private static final int BUFFER_CAPACITY = 1_024;
  private static final boolean IS_BLOCKING = false;

  public static void main(String[] args) {
    System.out.println("Redis-like server starting on port " + PORT + "...");

    CommandHandler handler = new CommandHandler();
    DataStore dataStore = new DataStore();

    try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
        Selector selector = Selector.open()) {

      serverChannel.configureBlocking(IS_BLOCKING);
      serverChannel.bind(new InetSocketAddress(PORT));
      serverChannel.register(selector, SelectionKey.OP_ACCEPT);

      while (true) {
        selector.select();
        Set<SelectionKey> readyKeys = selector.selectedKeys();
        Iterator<SelectionKey> iterator = readyKeys.iterator();

        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          iterator.remove();

          if (key.isAcceptable()) {
            acceptConnection(key, selector);
          } else if (key.isReadable()) {
            readFromClient(key, handler, dataStore);
          }
        }
      }

    } catch (IOException e) {
      System.err.println("IOException: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void acceptConnection(SelectionKey key, Selector selector) throws IOException {
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel clientChannel = serverChannel.accept();

    if (clientChannel != null) {
      clientChannel.configureBlocking(IS_BLOCKING);
      clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_CAPACITY));
      System.out.println("Accepted connection from " + clientChannel.getRemoteAddress());
    }
  }

  private static void readFromClient(SelectionKey key, CommandHandler handler, DataStore dataStore)
      throws IOException {

    SocketChannel clientChannel = (SocketChannel) key.channel();
    ByteBuffer buffer = (ByteBuffer) key.attachment();

    int bytesRead = clientChannel.read(buffer);
    if (bytesRead > 0) {
      buffer.flip();

      String[] commands = RESPParser.parse(buffer);
      ByteBuffer response = handler.handle(commands, dataStore);

      while (response.hasRemaining()) {
        clientChannel.write(response);
      }

      buffer.clear();

    } else if (bytesRead == -1) {
      System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
      key.cancel();
      clientChannel.close();
    }
  }
}
