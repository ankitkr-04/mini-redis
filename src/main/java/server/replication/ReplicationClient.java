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

    public enum HandshakeState {
        INITIAL, PING_SENT, PING_RECEIVED, REPLCONF_PORT_SENT, REPLCONF_PORT_RECEIVED, REPLCONF_CAPA_SENT, REPLCONF_CAPA_RECEIVED, PSYNC_SENT, PSYNC_RECEIVED, RDB_RECEIVING, REPLICATION_ACTIVE
    }

    private HandshakeState state;
    private final MasterInfo masterInfo;
    private final ReplicationInfo replicationInfo;
    private final int replicaPort;
    private SocketChannel masterChannel;
    private ByteBuffer readBuffer;
    private int expectedRdbSize = -1;
    private ByteBuffer rdbBuffer;

    public ReplicationClient(MasterInfo masterInfo, ReplicationInfo replicationInfo,
            int replicaPort) {
        this.state = HandshakeState.INITIAL;
        this.masterInfo = masterInfo;
        this.replicationInfo = replicationInfo;
        this.replicaPort = replicaPort;
        this.readBuffer = ByteBuffer.allocate(8192);
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
        sendCommand("PING");
        System.out.println("Sent PING to master");
    }

    private void sendReplconfPort() throws IOException {
        sendCommand("REPLCONF", "listening-port", String.valueOf(getReplicaPort()));
        System.out.println("Sent REPLCONF listening-port");
    }

    private void sendReplconfCapa() throws IOException {
        sendCommand("REPLCONF", "capa", "eof", "capa", "psync2");
        System.out.println("Sent REPLCONF capabilities");
    }

    private void sendPsync() throws IOException {
        sendCommand("PSYNC", "?", "-1");
        System.out.println("Sent PSYNC for full resync");
    }

    private void sendCommand(String... args) throws IOException {
        if (!masterChannel.isOpen() || !masterChannel.isConnected()) {
            throw new IOException("Master channel is not open or connected");
        }

        ByteBuffer command = ResponseBuilder.array(List.of(args));
        int bytesWritten = masterChannel.write(command);

        if (bytesWritten <= 0) {
            System.err.println("Failed to write command to master: " + String.join(" ", args));
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

            if (state == HandshakeState.RDB_RECEIVING) {
                handleRdbData();
            } else {
                handleHandshakeResponse();
            }
        }
    }

    private void handleHandshakeResponse() throws IOException {
        String response = new String(readBuffer.array(), 0, readBuffer.limit());
        System.out.println(
                "Received from master (" + state + "): " + response.replace("\r\n", "\\r\\n"));

        switch (state) {
            case INITIAL -> {
                // No specific action for INITIAL state
            }
            case PING_RECEIVED -> {
                // No specific action for PING_RECEIVED state
            }
            case REPLCONF_PORT_RECEIVED -> {
                // No specific action for REPLCONF_PORT_RECEIVED state
            }
            case REPLCONF_CAPA_RECEIVED -> {
                // No specific action for REPLCONF_CAPA_RECEIVED state
            }
            case PSYNC_RECEIVED -> {
                // No specific action for PSYNC_RECEIVED state
            }
            case RDB_RECEIVING -> {
                // No specific action for RDB_RECEIVING state
            }
            case REPLICATION_ACTIVE -> {
                // No specific action for REPLICATION_ACTIVE state
            }
            case PING_SENT -> {
                if (response.startsWith("+PONG")) {
                    System.out.println("✓ PING handshake successful");
                    state = HandshakeState.PING_RECEIVED;
                    sendReplconfPort();
                    state = HandshakeState.REPLCONF_PORT_SENT;
                } else {
                    handleHandshakeError("Unexpected PING response: " + response);
                }
            }

            case REPLCONF_PORT_SENT -> {
                if (response.startsWith("+OK")) {
                    System.out.println("✓ REPLCONF port handshake successful");
                    state = HandshakeState.REPLCONF_PORT_RECEIVED;
                    sendReplconfCapa();
                    state = HandshakeState.REPLCONF_CAPA_SENT;
                } else {
                    handleHandshakeError("Unexpected REPLCONF port response: " + response);
                }
            }

            case REPLCONF_CAPA_SENT -> {
                if (response.startsWith("+OK")) {
                    System.out.println("✓ REPLCONF capabilities handshake successful");
                    state = HandshakeState.REPLCONF_CAPA_RECEIVED;
                    sendPsync();
                    state = HandshakeState.PSYNC_SENT;
                } else {
                    handleHandshakeError("Unexpected REPLCONF capabilities response: " + response);
                }
            }

            case PSYNC_SENT -> {
                if (response.startsWith("+FULLRESYNC")) {
                    handleFullResyncResponse(response);
                } else if (response.startsWith("+CONTINUE")) {
                    handlePartialResyncResponse(response);
                } else {
                    handleHandshakeError("Unexpected PSYNC response: " + response);
                }
            }
        }
    }

    private void handleFullResyncResponse(String response) throws IOException {
        // Parse FULLRESYNC response: +FULLRESYNC <replid> <offset>
        String[] parts = response.trim().split("\\s+");
        if (parts.length >= 3) {
            String replId = parts[1];
            String offset = parts[2];
            System.out.println("✓ PSYNC handshake successful - Full resync");
            System.out.println("  Master replication ID: " + replId);
            System.out.println("  Master offset: " + offset);

            state = HandshakeState.PSYNC_RECEIVED;
            // Next, we expect to receive the RDB file
            prepareForRdbReceiving();
        } else {
            handleHandshakeError("Invalid FULLRESYNC response format: " + response);
        }
    }

    private void handlePartialResyncResponse(String response) {
        System.out.println("✓ PSYNC handshake successful - Partial resync");
        state = HandshakeState.REPLICATION_ACTIVE;
        replicationInfo.setHandshakeCompleted(true);
        System.out.println("🎉 Replication handshake completed successfully!");
    }

    private void prepareForRdbReceiving() throws IOException {
        // The next data should be the RDB file in bulk string format: $<size>\r\n<data>
        state = HandshakeState.RDB_RECEIVING;
        System.out.println("Preparing to receive RDB file...");
    }

    private void handleRdbData() throws IOException {
        if (expectedRdbSize == -1) {
            // Parse RDB size from bulk string header
            String header = new String(readBuffer.array(), 0, readBuffer.limit());
            if (header.startsWith("$")) {
                int crlfPos = header.indexOf("\r\n");
                if (crlfPos != -1) {
                    try {
                        expectedRdbSize = Integer.parseInt(header.substring(1, crlfPos));
                        rdbBuffer = ByteBuffer.allocate(expectedRdbSize);
                        System.out.println(
                                "Expecting RDB file of size: " + expectedRdbSize + " bytes");

                        // Check if we already have some RDB data in this buffer
                        int headerSize = crlfPos + 2;
                        if (readBuffer.limit() > headerSize) {
                            byte[] rdbData = new byte[readBuffer.limit() - headerSize];
                            readBuffer.position(headerSize);
                            readBuffer.get(rdbData);
                            rdbBuffer.put(rdbData);
                            System.out.println(
                                    "Received " + rdbData.length + " RDB bytes in first chunk");
                        }
                    } catch (NumberFormatException e) {
                        handleHandshakeError("Invalid RDB size format: " + header);
                    }
                }
            }
        } else {
            // Continue receiving RDB data
            byte[] data = new byte[readBuffer.limit()];
            readBuffer.get(data);
            rdbBuffer.put(data);
            System.out.println("Received " + data.length + " more RDB bytes (" +
                    rdbBuffer.position() + "/" + expectedRdbSize + ")");
        }

        // Check if we've received the complete RDB file
        if (rdbBuffer != null && rdbBuffer.position() >= expectedRdbSize) {
            System.out
                    .println("✓ RDB file received completely (" + rdbBuffer.position() + " bytes)");
            state = HandshakeState.REPLICATION_ACTIVE;
            replicationInfo.setHandshakeCompleted(true);
            System.out.println("🎉 Replication handshake completed successfully!");
        }
    }

    private void handleHandshakeError(String errorMessage) throws IOException {
        System.err.println("❌ Replication handshake failed: " + errorMessage);
        masterChannel.close();
    }

    private int getReplicaPort() {
        // Return the replica's listening port
        // This should be obtained from ServerContext
        return 6380; // Default replica port for now
    }

    public boolean isHandshakeCompleted() {
        return state == HandshakeState.REPLICATION_ACTIVE;
    }

    public HandshakeState getState() {
        return state;
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
