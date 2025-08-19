import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Main {

  private static String[] parseBuffer(ByteBuffer buffer) {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    String str = new String(bytes, StandardCharsets.UTF_8);

    if (str.isEmpty()) return new String[] {};

    // RESP array starts with '*'
    if (str.charAt(0) == '*') {
        String[] lines = str.split("\r\n");
        if (lines.length < 2) return new String[] {};

        try {
            int arraySize = Integer.parseInt(lines[0].substring(1));
            String[] result = new String[arraySize];
            int idx = 0;

            for (int i = 1; i < lines.length; i++) {
                if (lines[i].startsWith("$")) {
                    int len = Integer.parseInt(lines[i].substring(1));
                    // next line is actual content
                    if (i + 1 < lines.length) {
                        result[idx++] = lines[i + 1];
                        i++; // skip content line
                    }
                }
            }
            return result;
        } catch (NumberFormatException e) {
            return new String[] {};
        }
    }

    // Simple string without array
    return new String[] { str.trim() };
}


  private static ByteBuffer formatMessage(String[] commands) {
    int len = commands.length;

    if (commands[0].equalsIgnoreCase("PING")) {
      return ByteBuffer.wrap("+PONG\r\n".getBytes());
    }

    if (commands[0].equalsIgnoreCase("ECHO")) {

      if (len - 1 != 1)
        return ByteBuffer
            .wrap(("-ERR wrong number of arguments for 'echo' command\r\n").getBytes());

      String msg = commands[1];
      String resp = "$" + msg.length() + "\r\n" + msg + "\r\n";
      return ByteBuffer.wrap(resp.getBytes());

    }

    return ByteBuffer.wrap(("-ERR unknown command\r\n").getBytes());
  }

  public static void main(String[] args) {
    final int PORT = 6379;
    final int BUFFER_CAPACITY = 1_024;
    boolean listening = true;
    boolean isBlocking = false;

    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");



    try {
      ServerSocketChannel serverChannel = ServerSocketChannel.open();
      serverChannel.configureBlocking(isBlocking);
      serverChannel.bind(new InetSocketAddress(PORT));

      Selector selector = Selector.open();
      serverChannel.register(selector, SelectionKey.OP_ACCEPT);

      while (listening) {
        selector.select();
        Set<SelectionKey> readyKeys = selector.selectedKeys();

        for (var key : readyKeys) {
          if (key.isAcceptable()) {
            // Accept New Connections
            var newServerChannel = (ServerSocketChannel) key.channel();
            // Non-blocking clientChnnel
            var clientChannel = newServerChannel.accept();

            // If client not null, register it
            if (clientChannel != null) {
              clientChannel.configureBlocking(isBlocking);
              clientChannel.register(selector, SelectionKey.OP_READ,
                  ByteBuffer.allocate(BUFFER_CAPACITY));
            }

          } else if (key.isReadable()) {
            var clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();

            int bytesRead = clientChannel.read(buffer);
            // Something is in clientChnnel input
            if (bytesRead > 0) {
              buffer.flip();
              var strings = parseBuffer(buffer);

              var result = formatMessage(strings);
              clientChannel.write(result);

              buffer.clear();
            } else if (bytesRead == -1) {
              key.cancel();
              clientChannel.close();
            }
          }

        }

        readyKeys.clear();


      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
