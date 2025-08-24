package replication;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReplicationState {
    private static final Logger log = LoggerFactory.getLogger(ReplicationState.class);

    public enum Role {
        MASTER, SLAVE
    }

    public enum HandshakeStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

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

    public ReplicationState(boolean isReplica, String masterHost, int masterPort, long backlogSize) {
        this.role = isReplica ? Role.SLAVE : Role.MASTER;
        this.masterReplicationId = isReplica ? "?" : generateReplicationId();
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.backlogSize = backlogSize;

        log.info("Initialized replication state - Role: {}, Master: {}:{}", role, masterHost, masterPort);
    }

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

    // Getters
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

    // Setters
    public void setMasterReplicationId(String id) {
        this.masterReplicationId = id;
    }

    public void setHandshakeStatus(HandshakeStatus status) {
        this.handshakeStatus = status;
        if (status == HandshakeStatus.COMPLETED) {
            log.info("Replication handshake completed successfully");
        }
    }

    // Operations
    public void incrementSlaves() {
        int current = connectedSlaves.incrementAndGet();
        log.debug("Incremented slave count to: {}", current);
    }

    public void decrementSlaves() {
        int current = connectedSlaves.decrementAndGet();
        log.debug("Decremented slave count to: {}", current);
    }

    public void incrementReplicationOffset(long bytes) {
        long newOffset = masterReplicationOffset.addAndGet(bytes);
        log.trace("Incremented replication offset by {} to {}", bytes, newOffset);
    }

    private static String generateReplicationId() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(40);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}