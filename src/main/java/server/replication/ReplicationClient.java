package server.replication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;
import config.CommandLineParser.MasterInfo;
import protocol.ResponseBuilder;

public class ReplicationClient {

    private enum HandshakeState {
        INITIAL, PING_SENT, PING_RECEIVED, REPLCONF_SENT, REPLCONF_ACK, PSYNC_SENT, PSYNC_RECEIVED
    }

    private HandshakeState state;
    private final MasterInfo masterInfo;
    private final ReplicationInfo replicationInfo;
    private SocketChannel masterChannel;
    private ByteBuffer readBuffer;

    public ReplicationClient(MasterInfo masterInfo, ReplicationInfo replicationInfo) {
        this.state = HandshakeState.INITIAL;
        this.masterInfo = masterInfo;
        this.replicationInfo = replicationInfo;
        this.readBuffer = ByteBuffer.allocate(1024);
    }

    public void register(Selector selector) throws IOException {
        if (state != HandshakeState.INITIAL) {
            throw new IllegalStateException("Already started or invalid state");
        }
        masterChannel = SocketChannel.open();
        masterChannel.configureBlocking(false);
        System.out.println(
                "Attempting to connect to master " + masterInfo.host() + ":" + masterInfo.port());
        masterChannel.connect(new InetSocketAddress(masterInfo.host(), masterInfo.port()));
        masterChannel.register(selector, SelectionKey.OP_CONNECT, this);
        System.out.println("Registered master channel for connection");
        selector.wakeup();
    }

    public void handleKey(SelectionKey key) throws IOException {
        if (key.isConnectable()) {
            handleConnect(key);
        } else if (key.isReadable()) {
            handleRead(key);
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        var channel = (SocketChannel) key.channel();
        if (channel.finishConnect()) {
            System.out
                    .println("Connected to master " + masterInfo.host() + ":" + masterInfo.port());
            sendPing();
            state = HandshakeState.PING_SENT;
            key.interestOps(SelectionKey.OP_READ);
        } else {
            key.interestOps(SelectionKey.OP_CONNECT);
        }
    }

    private void sendPing() throws IOException {
        if (!masterChannel.isOpen() || !masterChannel.isConnected()) {
            throw new IOException("Master channel is not open or connected");
        }
        ByteBuffer ping = ResponseBuilder.array(List.of("PING"));
        System.out.println(
                "Sending PING: " + new String(ping.array(), ping.position(), ping.limit()));
        int bytesWritten = masterChannel.write(ping);
        if (bytesWritten <= 0) {
            System.err.println("Failed to write PING to master.");
        } else {
            System.out.println("Sent PING to master (" + bytesWritten + " bytes).");
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        var channel = (SocketChannel) key.channel();
        readBuffer.clear();
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            System.out.println("Master closed the connection.");
            channel.close();
            key.cancel();
            return;
        }
        if (bytesRead > 0) {
            readBuffer.flip();
            String response = new String(readBuffer.array(), 0, readBuffer.limit());
            readBuffer.clear();
            System.out.println("Received from master: " + response);
            if (state == HandshakeState.PING_SENT) {
                if (response.equals("+PONG\r\n")) {
                    System.out.println("Received PONG from master.");
                    state = HandshakeState.PING_RECEIVED;
                    // Keep channel open for future handshake steps
                    // TODO: Implement REPLCONF and PSYNC for full handshake
                } else {
                    System.err.println("Unexpected response to PING: " + response);
                    channel.close();
                    key.cancel();
                }
            }
        }
    }

    public void shutdown() {
        try {
            if (masterChannel != null && masterChannel.isOpen()) {
                masterChannel.close();
            }
            System.out.println("Replication client shut down.");
        } catch (IOException e) {
            System.err.println("Error closing replication client: " + e.getMessage());
        }
    }
}
