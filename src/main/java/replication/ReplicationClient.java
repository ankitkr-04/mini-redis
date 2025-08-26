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
import config.ServerConfig;
import protocol.ProtocolParser;
import server.ServerContext;

/**
 * Client responsible for connecting to and synchronizing with a Redis master
 * server.
 * 
 * <p>
 * This class handles the complete replication lifecycle including initial
 * handshake,
 * full synchronization, and ongoing incremental synchronization. It manages the
 * connection state, protocol negotiations, and data transfer with the master
 * server.
 * </p>
 * 
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public final class ReplicationClient {

    /** Logger instance for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationClient.class);

    private final MasterInfo masterInfo;
    private final ReplicationState replicationState;
    private final int replicaPort;
    private final ServerContext context;
    private final HandshakeManager handshakeManager;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(ServerConfig.BUFFER_SIZE);
    private SocketChannel masterChannel;

    /**
     * Constructs a new ReplicationClient.
     * 
     * @param masterInfo       information about the master server
     * @param replicationState the replication state tracker
     * @param replicaPort      the port this replica is listening on
     * @param context          the server context containing shared resources
     */
    public ReplicationClient(MasterInfo masterInfo, ReplicationState replicationState,
            int replicaPort, ServerContext context) {
        this.masterInfo = masterInfo;
        this.replicationState = replicationState;
        this.replicaPort = replicaPort;
        this.context = context;
        this.handshakeManager = new HandshakeManager();
        LOGGER.info("Created replication client for master {}:{}", masterInfo.host(), masterInfo.port());
    }

    /**
     * Registers this client with the selector and initiates connection to master.
     * 
     * @param selector the NIO selector for managing channels
     * @throws IOException if connection setup fails
     */
    public void register(Selector selector) throws IOException {
        if (handshakeManager.getState() != ReplicationProtocol.HandshakeState.INITIAL) {
            throw new IllegalStateException("Already started");
        }

        masterChannel = SocketChannel.open();
        masterChannel.configureBlocking(false);
        masterChannel.connect(new InetSocketAddress(masterInfo.host(), masterInfo.port()));
        masterChannel.register(selector, SelectionKey.OP_CONNECT, this);
        LOGGER.info("Connecting to master {}:{}", masterInfo.host(), masterInfo.port());
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
            LOGGER.error("Error closing replication client", e);
        }
    }

    private void handleConnect() throws IOException {
        if (masterChannel.finishConnect()) {
            LOGGER.info("Connected to master, starting handshake");
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
            LOGGER.info("HandshakeManager: current state = {}, buffer has {} bytes", state, buffer.remaining());

            // Process data based on current state, potentially multiple times if state
            // changes
            while (buffer.hasRemaining()) {
                int initialPosition = buffer.position();

                switch (state) {
                    case PING_SENT -> handlePingResponse(buffer);
                    case REPLCONF_PORT_SENT -> handleReplconfPortResponse(buffer);
                    case REPLCONF_CAPA_SENT -> handleReplconfCapaResponse(buffer);
                    case PSYNC_SENT -> handlePsyncResponse(buffer);
                    case RDB_RECEIVING -> handleRdbData(buffer);
                    case ACTIVE -> handleActiveReplication(buffer);
                    default -> LOGGER.warn("Unexpected state: {}", state);
                }

                // If no data was consumed, break to avoid infinite loop
                if (buffer.position() == initialPosition) {
                    break;
                }
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
            if (response == null) {
                LOGGER.debug("PSYNC response not complete yet, waiting for more data");
                return;
            }

            LOGGER.info("Received PSYNC response: {}", response);
            String[] parts = response.split(" ");
            if (parts.length != 3 || !"FULLRESYNC".equals(parts[0])) {
                throw new IOException("Unexpected PSYNC response: " + response);
            }

            String masterId = parts[1];
            long offset = Long.parseLong(parts[2]);
            replicationState.setMasterReplicationId(masterId);
            replicationState.incrementReplicationOffset(offset - replicationState.getMasterReplicationOffset());
            LOGGER.info("Full resync initiated with ID {} offset {}", masterId, offset);
            LOGGER.info("Transitioning to RDB_RECEIVING state, buffer has {} bytes remaining", buffer.remaining());
            state = ReplicationProtocol.HandshakeState.RDB_RECEIVING;
        }

        private void handleRdbData(ByteBuffer buffer) {
            if (expectedRdbSize == -1) {
                if (!buffer.hasRemaining() || buffer.get() != (byte) '$') {
                    LOGGER.error("Invalid RDB header - no $ marker");
                    return;
                }

                Long size = parseNumber(buffer);
                if (size == null) {
                    LOGGER.debug("Could not parse RDB size yet, waiting for more data");
                    return;
                }

                expectedRdbSize = size.intValue();
                rdbBuffer = ByteBuffer.allocate(expectedRdbSize);
                LOGGER.info("Expecting RDB file of {} bytes", expectedRdbSize);
            }

            int remaining = Math.min(expectedRdbSize - rdbBuffer.position(), buffer.remaining());
            if (remaining > 0) {
                byte[] data = new byte[remaining];
                buffer.get(data);
                rdbBuffer.put(data);
                LOGGER.debug("Read {} bytes of RDB data, total: {}/{}", remaining, rdbBuffer.position(),
                        expectedRdbSize);
            }

            if (rdbBuffer.position() == expectedRdbSize) {
                LOGGER.info("RDB file received completely ({} bytes), transitioning to ACTIVE state", expectedRdbSize);
                state = ReplicationProtocol.HandshakeState.ACTIVE;
                replicationState.setHandshakeStatus(ReplicationState.HandshakeStatus.COMPLETED);

                // Process any remaining data in the buffer as active replication commands
                if (buffer.hasRemaining()) {
                    LOGGER.info("Processing remaining {} bytes as active replication commands", buffer.remaining());
                    handleActiveReplication(buffer);
                }
            }
        }

        private void handleActiveReplication(ByteBuffer buffer) {
            List<String[]> commands = ProtocolParser.parseRespArrays(buffer);
            for (String[] command : commands) {
                LOGGER.info("Processing replication command: {}", String.join(" ", command));
                ByteBuffer response = context.getCommandDispatcher().dispatch(command, null, true);
                if (response != null) {
                    LOGGER.info("Got response for command {}, sending {} bytes to master", command[0],
                            response.remaining());
                    try {
                        ReplicationProtocol.sendResponse(masterChannel, response);
                        LOGGER.info("Successfully sent response to master");
                    } catch (IOException e) {
                        LOGGER.error("Failed to send response to master", e);
                    }
                } else {
                    LOGGER.info("No response generated for command {}", command[0]);
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