package server.replication;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ProtocolConstants;
import config.ProtocolConstants.Role;
import protocol.parser.CommandLineParser.MasterInfo;

public class ReplicationInfo {
    private static final Logger log = LoggerFactory.getLogger(ReplicationInfo.class);

    private volatile Role role;
    private final String masterReplId;
    private final AtomicInteger connectedSlaves;
    private final AtomicLong masterReplOffset;
    private final AtomicBoolean replBacklogActive;
    private final long replBacklogSize;
    private final AtomicLong replBacklogFirstByteOffset;
    private final AtomicLong replBacklogHistlen;
    private final String masterHost;
    private final int masterPort;
    private volatile boolean handshakeCompleted;

    public ReplicationInfo(Optional<MasterInfo> masterInfo, long replBacklogSize) {
        this.role = masterInfo.isPresent() ? Role.SLAVE : Role.MASTER;
        this.masterReplId = generateReplId();
        this.connectedSlaves = new AtomicInteger(0);
        this.masterReplOffset = new AtomicLong(0);
        this.replBacklogActive = new AtomicBoolean(false);
        this.replBacklogSize = replBacklogSize;
        this.replBacklogFirstByteOffset = new AtomicLong(0);
        this.replBacklogHistlen = new AtomicLong(0);
        this.masterHost = masterInfo.map(MasterInfo::host).orElse(null);
        this.masterPort = masterInfo.map(MasterInfo::port).orElse(0);
        this.handshakeCompleted = false;

        log.info("Initialized replication info - Role: {}, Master: {}:{}",
                role, masterHost, masterPort);
    }

    public void setHandshakeCompleted(boolean completed) {
        this.handshakeCompleted = completed;
        if (completed) {
            log.info("Replication handshake completed successfully");
        }
    }

    public Map<String, String> toInfoMap() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put(ProtocolConstants.ROLE_FIELD, role.getValue());
        info.put(ProtocolConstants.MASTER_REPLID_FIELD, masterReplId);
        info.put(ProtocolConstants.CONNECTED_SLAVES_FIELD, String.valueOf(connectedSlaves.get()));
        info.put(ProtocolConstants.MASTER_REPL_OFFSET_FIELD, String.valueOf(masterReplOffset.get()));
        info.put(ProtocolConstants.MASTER_HOST_FIELD, masterHost != null ? masterHost : "");
        info.put(ProtocolConstants.MASTER_PORT_FIELD, String.valueOf(masterPort));
        info.put(ProtocolConstants.HANDSHAKE_STATUS_FIELD, handshakeCompleted ? "completed" : "in_progress");

        // backlog details
        info.put("repl_backlog_active", String.valueOf(replBacklogActive.get() ? 1 : 0));
        info.put("repl_backlog_size", String.valueOf(replBacklogSize));
        info.put("repl_backlog_first_byte_offset", String.valueOf(replBacklogFirstByteOffset.get()));
        info.put("repl_backlog_histlen", String.valueOf(replBacklogHistlen.get()));

        return info;
    }

    // Getters and setters
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getMasterReplId() {
        return masterReplId;
    }

    public int getConnectedSlaves() {
        return connectedSlaves.get();
    }

    public long getMasterReplOffset() {
        return masterReplOffset.get();
    }

    public boolean isReplBacklogActive() {
        return replBacklogActive.get();
    }

    public long getReplBacklogSize() {
        return replBacklogSize;
    }

    public long getReplBacklogFirstByteOffset() {
        return replBacklogFirstByteOffset.get();
    }

    public long getReplBacklogHistlen() {
        return replBacklogHistlen.get();
    }

    public String getMasterHost() {
        return masterHost;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public boolean isHandshakeCompleted() {
        return handshakeCompleted;
    }

    public void incrementSlaves() {
        int current = connectedSlaves.incrementAndGet();
        log.debug("Incremented slave count to: {}", current);
    }

    public void decrementSlaves() {
        int current = connectedSlaves.decrementAndGet();
        log.debug("Decremented slave count to: {}", current);
    }

    public void incrementReplOffset(long bytes) {
        long newOffset = masterReplOffset.addAndGet(bytes);
        log.trace("Incremented replication offset by {} to {}", bytes, newOffset);
    }

    private static String generateReplId() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(40);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
