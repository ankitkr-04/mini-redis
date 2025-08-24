package server.replication;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import config.CommandLineParser.MasterInfo;

public class ReplicationInfo {
    public static final String ROLE = "role";
    public static final String MASTER_REPLID = "master_replid";
    public static final String CONNECTED_SLAVES = "connected_slaves";
    public static final String MASTER_REPL_OFFSET = "master_repl_offset";
    public static final String REPL_BACKLOG_ACTIVE = "repl_backlog_active";
    public static final String REPL_BACKLOG_SIZE = "repl_backlog_size";
    public static final String REPL_BACKLOG_FIRST_BYTE_OFFSET = "repl_backlog_first_byte_offset";
    public static final String REPL_BACKLOG_HISTLEN = "repl_backlog_histlen";
    public static final String MASTER_HOST = "master_host";
    public static final String MASTER_PORT = "master_port";
    public static final String HANDSHAKE_STATUS = "handshake_status";

    private String role;
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

    public enum Role {
        MASTER("master"), SLAVE("slave");

        private final String role;

        Role(String role) {
            this.role = role;
        }

        public String getRole() {
            return role;
        }
    }

    public ReplicationInfo(Optional<MasterInfo> masterInfo, long replBacklogSize) {
        this.role = masterInfo.isPresent() ? Role.SLAVE.getRole() : Role.MASTER.getRole();
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
    }

    public void setHandshakeCompleted(boolean completed) {
        this.handshakeCompleted = completed;
    }

    public Map<String, String> toInfoMap() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put(ROLE, role);
        info.put(MASTER_REPLID, masterReplId);
        info.put(CONNECTED_SLAVES, String.valueOf(connectedSlaves.get()));
        info.put(MASTER_REPL_OFFSET, String.valueOf(masterReplOffset.get()));
        info.put(REPL_BACKLOG_ACTIVE, String.valueOf(replBacklogActive.get() ? 1 : 0));
        info.put(REPL_BACKLOG_SIZE, String.valueOf(replBacklogSize));
        info.put(REPL_BACKLOG_FIRST_BYTE_OFFSET, String.valueOf(replBacklogFirstByteOffset.get()));
        info.put(REPL_BACKLOG_HISTLEN, String.valueOf(replBacklogHistlen.get()));
        if (masterHost != null) {
            info.put(MASTER_HOST, masterHost);
        }
        if (masterPort != 0) {
            info.put(MASTER_PORT, String.valueOf(masterPort));
        }
        info.put(HANDSHAKE_STATUS, handshakeCompleted ? "completed" : "in_progress");
        return info;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
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

    public void incrementSlaves() {
        connectedSlaves.incrementAndGet();
    }

    public void decrementSlaves() {
        connectedSlaves.decrementAndGet();
    }

    public void incrementReplOffset(long bytes) {
        masterReplOffset.addAndGet(bytes);
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
