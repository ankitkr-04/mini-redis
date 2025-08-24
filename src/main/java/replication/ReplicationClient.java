package replication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ConfigurationParser.MasterInfo;
import protocol.ProtocolParser;
import server.ServerContext;

public final class ReplicationClient {
    private static final Logger log = LoggerFactory.getLogger(ReplicationClient.class);
    private static final int BUFFER_SIZE = 1024;

    private final MasterInfo masterInfo;
    private final ReplicationState replicationState;
    private final int replicaPort;
    private final ServerContext context;
    private final HandshakeManager handshakeManager;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private SocketChannel masterChannel;

    public ReplicationClient(MasterInfo masterInfo, ReplicationState replicationState,
            int replicaPort, ServerContext context) {
        this.masterInfo = masterInfo;
        this.replicationState = replicationState;
        this.replicaPort = replicaPort;
        this.context = context;
        this.handshakeManager = new HandshakeManager();
        log.info("Created replication client for master {}:{}", masterInfo.host(), masterInfo.port());
    }

    public void register(Selector selector) throws IOException {
        if (handshakeManager.getState() != ReplicationProtocol.HandshakeState.INITIAL) {
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
            handleConnect();
            key.interestOps(SelectionKey.OP_READ);
        } else if (key.isReadable()) {
            handleRead();
        }
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

    private void handleConnect() throws IOException {
        if (masterChannel.finishConnect()) {
            log.info("Connected to master, starting handshake");
            handshakeManager.startHandshake();
        }
    }

    private void handleRead() throws IOException {
        int bytesRead = masterChannel.read(readBuffer);
        if (bytesRead == -1) {
            throw new IOException("Master connection closed");
        }
        if (bytesRead == 0)
            return;

        readBuffer.flip();
        handshakeManager.handleIncomingData(readBuffer);
        readBuffer.compact();
    }

    private class HandshakeManager {
        private ReplicationProtocol.HandshakeState state = ReplicationProtocol.HandshakeState.INITIAL;
        private int expectedRdbSize = -1;
        private ByteBuffer rdbBuffer;

        public void startHandshake() throws IOException {
            ReplicationProtocol.sendCommand(masterChannel, "PING");
            state = ReplicationProtocol.HandshakeState.PING_SENT;
        }

        public void handleIncomingData(ByteBuffer buffer) throws IOException {
            switch (state) {
                case PING_SENT -> handlePingResponse(buffer);
                case REPLCONF_PORT_SENT -> handleReplconfPortResponse(buffer);
                case REPLCONF_CAPA_SENT -> handleReplconfCapaResponse(buffer);
                case PSYNC_SENT -> handlePsyncResponse(buffer);
                case RDB_RECEIVING -> handleRdbData(buffer);
                case ACTIVE -> handleActiveReplication(buffer);
                default -> log.warn("Unexpected state: {}", state);
            }
        }

        private void handlePingResponse(ByteBuffer buffer) throws IOException {
            String response = ProtocolParser.parseSimpleString(buffer);
            if (response == null)
                return;

            if (!"PONG".equals(response)) {
                throw new IOException("Invalid PING response: " + response);
            }

            ReplicationProtocol.sendCommand(masterChannel, "REPLCONF", "listening-port", String.valueOf(replicaPort));
            state = ReplicationProtocol.HandshakeState.REPLCONF_PORT_SENT;
        }

        private void handleReplconfPortResponse(ByteBuffer buffer) throws IOException {
            String response = ProtocolParser.parseSimpleString(buffer);
            if (response == null)
                return;

            if (!"OK".equals(response)) {
                throw new IOException("Invalid REPLCONF port response: " + response);
            }

            ReplicationProtocol.sendCommand(masterChannel, "REPLCONF", "capa", "eof", "capa", "psync2");
            state = ReplicationProtocol.HandshakeState.REPLCONF_CAPA_SENT;
        }

        private void handleReplconfCapaResponse(ByteBuffer buffer) throws IOException {
            String response = ProtocolParser.parseSimpleString(buffer);
            if (response == null)
                return;

            if (!"OK".equals(response)) {
                throw new IOException("Invalid REPLCONF capa response: " + response);
            }

            ReplicationProtocol.sendCommand(masterChannel, "PSYNC", "?", "-1");
            state = ReplicationProtocol.HandshakeState.PSYNC_SENT;
        }

        private void handlePsyncResponse(ByteBuffer buffer) throws IOException {
            String response = ProtocolParser.parseSimpleString(buffer);
            if (response == null)
                return;

            String[] parts = response.split(" ");
            if (parts.length != 3 || !"FULLRESYNC".equals(parts[0])) {
                throw new IOException("Unexpected PSYNC response: " + response);
            }

            String masterId = parts[1];
            long offset = Long.parseLong(parts[2]);
            replicationState.setMasterReplicationId(masterId);
            replicationState.incrementReplicationOffset(offset - replicationState.getMasterReplicationOffset());
            log.info("Full resync initiated with ID {} offset {}", masterId, offset);
            state = ReplicationProtocol.HandshakeState.RDB_RECEIVING;
        }

        private void handleRdbData(ByteBuffer buffer) {
            if (expectedRdbSize == -1) {
                if (!buffer.hasRemaining() || buffer.get() != (byte) '$') {
                    log.error("Invalid RDB header");
                    return;
                }

                Long size = parseNumber(buffer);
                if (size == null)
                    return;

                expectedRdbSize = size.intValue();
                rdbBuffer = ByteBuffer.allocate(expectedRdbSize);
            }

            int remaining = Math.min(expectedRdbSize - rdbBuffer.position(), buffer.remaining());
            if (remaining > 0) {
                byte[] data = new byte[remaining];
                buffer.get(data);
                rdbBuffer.put(data);
            }

            if (rdbBuffer.position() == expectedRdbSize) {
                log.info("RDB file received completely ({} bytes), handshake complete", expectedRdbSize);
                state = ReplicationProtocol.HandshakeState.ACTIVE;
                replicationState.setHandshakeStatus(ReplicationState.HandshakeStatus.COMPLETED);
            }
        }

        private void handleActiveReplication(ByteBuffer buffer) {
            List<String[]> commands = ProtocolParser.parseRespArrays(buffer);
            for (String[] command : commands) {
                ByteBuffer response = context.getCommandDispatcher().dispatch(command, masterChannel, true);
                if (response != null) {
                    try {
                        ReplicationProtocol.sendResponse(masterChannel, response);
                    } catch (IOException e) {
                        log.error("Failed to send response to master", e);
                    }
                }
                replicationState.incrementReplicationOffset(ReplicationProtocol.calculateCommandSize(command));
            }
        }

        private Long parseNumber(ByteBuffer buffer) {
            StringBuilder sb = new StringBuilder();
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                if (b == (byte) '\r') {
                    if (buffer.hasRemaining() && buffer.get() == (byte) '\n') {
                        break;
                    }
                    return null;
                }
                sb.append((char) b);
            }

            try {
                return Long.parseLong(sb.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public ReplicationProtocol.HandshakeState getState() {
            return state;
        }
    }
}