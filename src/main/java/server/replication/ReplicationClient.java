package server.replication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ProtocolConstants;
import protocol.parser.CommandLineParser.MasterInfo;

public class ReplicationClient {
    private static final Logger log = LoggerFactory.getLogger(ReplicationClient.class);

    private final MasterInfo masterInfo;
    private final ReplicationInfo replicationInfo;
    private final int replicaPort;
    private final ReplicationService replicationService;
    private final HandshakeManager handshakeManager;
    private SocketChannel masterChannel;

    public ReplicationClient(MasterInfo masterInfo, ReplicationInfo replicationInfo, int replicaPort) {
        this.masterInfo = masterInfo;
        this.replicationInfo = replicationInfo;
        this.replicaPort = replicaPort;
        this.replicationService = new ReplicationService();
        this.handshakeManager = new HandshakeManager();
        log.info("Created replication client for master {}:{}", masterInfo.host(), masterInfo.port());
    }

    public void register(Selector selector) throws IOException {
        if (handshakeManager.getState() != ProtocolConstants.HandshakeState.INITIAL) {
            throw new IllegalStateException("Already started");
        }

        masterChannel = SocketChannel.open();
        masterChannel.configureBlocking(false);
        masterChannel.connect(new InetSocketAddress(masterInfo.host(), masterInfo.port()));
        masterChannel.register(selector, SelectionKey.OP_CONNECT, this);
        log.info("Connecting to master {}:{}", masterInfo.host(), masterInfo.port());
    }

    public void handleKey(SelectionKey key) throws IOException {
        if (key.isConnectable()) {
            handleConnect(key);
        } else if (key.isReadable()) {
            handleRead(key);
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.finishConnect()) {
            log.info("Connected to master, starting handshake");
            handshakeManager.startHandshake();
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(ProtocolConstants.RDB_BUFFER_SIZE);
            String response = replicationService.readResponse(masterChannel, buffer);
            handshakeManager.handleResponse(response, buffer);
        } catch (IOException e) {
            log.warn("Connection to master lost: {}", e.getMessage());
            key.channel().close();
            key.cancel();
        }
    }

    public boolean isHandshakeCompleted() {
        return handshakeManager.getState() == ProtocolConstants.HandshakeState.ACTIVE;
    }

    public void shutdown() {
        try {
            if (masterChannel != null && masterChannel.isOpen()) {
                masterChannel.close();
            }
        } catch (IOException e) {
            log.error("Error closing replication client", e);
        }
    }

    // Simplified HandshakeManager as inner class
    private class HandshakeManager {
        private ProtocolConstants.HandshakeState state = ProtocolConstants.HandshakeState.INITIAL;
        private int expectedRdbSize = -1;
        private ByteBuffer rdbBuffer;

        public void startHandshake() throws IOException {
            sendPing();
        }

        public void handleResponse(String response, ByteBuffer buffer) throws IOException {
            log.debug("Handshake state: {}, response: {}", state, response.trim());

            switch (state) {
                case PING_SENT -> handlePingResponse(response);
                case REPLCONF_PORT_SENT -> handleReplconfPortResponse(response);
                case REPLCONF_CAPA_SENT -> handleReplconfCapaResponse(response);
                case PSYNC_SENT -> handlePsyncResponse(response);
                case RDB_RECEIVING -> handleRdbData(buffer);
                default -> log.warn("Unexpected response in state {}: {}", state, response);
            }
        }

        private void sendPing() throws IOException {
            replicationService.sendCommand(masterChannel, "PING");
            state = ProtocolConstants.HandshakeState.PING_SENT;
        }

        private void handlePingResponse(String response) throws IOException {
            if (!response.startsWith("+PONG")) {
                throw new IOException("Invalid PING response: " + response);
            }
            replicationService.sendCommand(masterChannel, "REPLCONF", "listening-port", String.valueOf(replicaPort));
            state = ProtocolConstants.HandshakeState.REPLCONF_PORT_SENT;
        }

        private void handleReplconfPortResponse(String response) throws IOException {
            if (!response.startsWith("+OK")) {
                throw new IOException("Invalid REPLCONF port response: " + response);
            }
            replicationService.sendCommand(masterChannel, "REPLCONF", "capa", "eof", "capa", "psync2");
            state = ProtocolConstants.HandshakeState.REPLCONF_CAPA_SENT;
        }

        private void handleReplconfCapaResponse(String response) throws IOException {
            if (!response.startsWith("+OK")) {
                throw new IOException("Invalid REPLCONF capa response: " + response);
            }
            replicationService.sendCommand(masterChannel, "PSYNC",
                    ProtocolConstants.DEFAULT_PSYNC_REPLID, ProtocolConstants.DEFAULT_PSYNC_OFFSET);
            state = ProtocolConstants.HandshakeState.PSYNC_SENT;
        }

        private void handlePsyncResponse(String response) throws IOException {
            if (response.startsWith(ProtocolConstants.FULLRESYNC_PREFIX)) {
                log.info("Full resync initiated");
                state = ProtocolConstants.HandshakeState.RDB_RECEIVING;
            } else {
                throw new IOException("Unexpected PSYNC response: " + response);
            }
        }

        private void handleRdbData(ByteBuffer buffer) throws IOException {
            if (expectedRdbSize == -1) {
                // Parse RDB size from header
                String header = new String(buffer.array(), 0, buffer.limit());
                int crlfPos = header.indexOf(ProtocolConstants.CRLF);
                if (crlfPos != -1 && header.startsWith("$")) {
                    expectedRdbSize = Integer.parseInt(header.substring(1, crlfPos));
                    rdbBuffer = ByteBuffer.allocate(expectedRdbSize);
                    log.info("Expecting RDB file of {} bytes", expectedRdbSize);

                    // Handle any RDB data in the same buffer
                    int headerSize = crlfPos + 2;
                    if (buffer.limit() > headerSize) {
                        byte[] rdbData = new byte[buffer.limit() - headerSize];
                        buffer.position(headerSize);
                        buffer.get(rdbData);
                        rdbBuffer.put(rdbData);
                    }
                }
            } else {
                // Continue receiving RDB data
                byte[] data = new byte[buffer.limit()];
                buffer.get(data);
                rdbBuffer.put(data);
            }

            if (rdbBuffer != null && rdbBuffer.position() >= expectedRdbSize) {
                log.info("RDB file received completely, handshake complete");
                state = ProtocolConstants.HandshakeState.ACTIVE;
                replicationInfo.setHandshakeCompleted(true);
            }
        }

        public ProtocolConstants.HandshakeState getState() {
            return state;
        }
    }
}