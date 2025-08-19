import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class Main {
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

            //If client not null, register it
            if(clientChannel != null){
              clientChannel.configureBlocking(isBlocking);
              clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_CAPACITY));
            }

          } else if (key.isReadable()) {
            var clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();

            int bytesRead = clientChannel.read(buffer);
            // Something is in clientChnnel input
            if(bytesRead > 0){
              buffer.flip();

              ByteBuffer res = ByteBuffer.wrap("+PONG\r\n".getBytes());
              clientChannel.write(res);
              buffer.clear(); 
            } else if( bytesRead == -1){
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
