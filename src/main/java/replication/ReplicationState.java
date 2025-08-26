package replication;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains the state for master-replica synchronization in the replication
 * module.
 * Tracks role, replication IDs, offsets, backlog, and handshake status.
 */
public final class ReplicationState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationState.class);

    // Constants
    private static final int REPLICATION_ID_LENGTH = 20;
    private static final String UNKNOWN_REPLICATION_ID = "?";

    public enum Role {
        MASTER, SLAVE
    }

    public enum HandshakeStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

    // State fields
    private volatile Role role;
    private volatile String masterReplicationId;
    private final AtomicInteger connectedSlaves = new AtomicInteger(0);
    private final AtomicLong masterReplicationOffset = new AtomicLong(0);
    private final AtomicBoolean backlogActive = new AtomicBoolean(false);
    private final long backlogSize;
    private final AtomicLong backlogFirstByteOffset = new AtomicLong(0);
    private final AtomicLong backlogHistoryLength = new AtomicLong(0);
    private final String masterHost;
    private final int masterPort;
    private volatile HandshakeStatus handshakeStatus = HandshakeStatus.NOT_STARTED;

    /**
     * Initializes the replication state.
     *
     * @param isReplica   true if this node is a replica, false if master
     * @param masterHost  host of the master (if replica)
     * @param masterPort  port of the master (if replica)
     * @param backlogSize size of the replication backlog
     */
    public ReplicationState(boolean isReplica, String masterHost, int masterPort, long backlogSize) {
        this.role = isReplica ? Role.SLAVE : Role.MASTER;
        this.masterReplicationId = isReplica ? UNKNOWN_REPLICATION_ID : generateReplicationId();
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.backlogSize = backlogSize;

        LOGGER.info("ReplicationState initialized: role={}, masterHost={}, masterPort={}", role, masterHost,
                masterPort);
    }

    /**
     * Returns a map of replication info for monitoring or debugging.
     */
    public Map<String, String> toInfoMap() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("role", role.name().toLowerCase());
        info.put("master_replid", masterReplicationId);
        info.put("connected_slaves", String.valueOf(connectedSlaves.get()));
        info.put("master_repl_offset", String.valueOf(masterReplicationOffset.get()));
        info.put("master_host", masterHost != null ? masterHost : "");
        info.put("master_port", String.valueOf(masterPort));
        info.put("handshake_status", handshakeStatus.name().toLowerCase());
        info.put("repl_backlog_active", String.valueOf(backlogActive.get() ? 1 : 0));
        info.put("repl_backlog_size", String.valueOf(backlogSize));
        info.put("repl_backlog_first_byte_offset", String.valueOf(backlogFirstByteOffset.get()));
        info.put("repl_backlog_histlen", String.valueOf(backlogHistoryLength.get()));
        return info;
    }

    public Role getRole() {
        return role;
    }

    public String getMasterReplicationId() {
        return masterReplicationId;
    }

    public int getConnectedSlaves() {
        return connectedSlaves.get();
    }

    public long getMasterReplicationOffset() {
        return masterReplicationOffset.get();
    }

    public String getMasterHost() {
        return masterHost;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public HandshakeStatus getHandshakeStatus() {
        return handshakeStatus;
    }

    public boolean isHandshakeCompleted() {
        return handshakeStatus == HandshakeStatus.COMPLETED;
    }

    public void setMasterReplicationId(String replicationId) {
        this.masterReplicationId = replicationId;
    }

    /**
     * Updates the handshake status and logs completion.
     */
    public void setHandshakeStatus(HandshakeStatus status) {
        this.handshakeStatus = status;
        if (status == HandshakeStatus.COMPLETED) {
            LOGGER.info("Replication handshake completed");
        }
    }

    /**
     * Increments the count of connected slaves.
     */
    public void incrementConnectedSlaves() {
        int count = connectedSlaves.incrementAndGet();
        LOGGER.debug("Connected slaves incremented: {}", count);
    }

    /**
     * Decrements the count of connected slaves.
     */
    public void decrementConnectedSlaves() {
        int count = connectedSlaves.decrementAndGet();
        LOGGER.debug("Connected slaves decremented: {}", count);
    }

    /**
     * Increments the replication offset by the specified number of bytes.
     */
    public void incrementReplicationOffset(long bytes) {
        long offset = masterReplicationOffset.addAndGet(bytes);
        LOGGER.trace("Replication offset incremented by {}: {}", bytes, offset);
    }

    /**
     * Generates a random replication ID.
     */
    private static String generateReplicationId() {
        byte[] randomBytes = new byte[REPLICATION_ID_LENGTH];
        new SecureRandom().nextBytes(randomBytes);
        StringBuilder hexBuilder = new StringBuilder(REPLICATION_ID_LENGTH * 2);
        for (byte b : randomBytes) {
            hexBuilder.append(String.format("%02x", b));
        }
        return hexBuilder.toString();
    }
}