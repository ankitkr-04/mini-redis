package replication;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReplicationManager {
    private static final Logger log = LoggerFactory.getLogger(ReplicationManager.class);

    private final ConcurrentHashMap<SocketChannel, AtomicLong> replicaOffsets = new ConcurrentHashMap<>();
    private final ReplicationState replicationState;

    public ReplicationManager(ReplicationState replicationState) {
        this.replicationState = replicationState;
    }

    public void addReplica(SocketChannel channel) {
        replicaOffsets.put(channel, new AtomicLong(0));
        replicationState.incrementSlaves();
        log.info("Added replica: {}, total: {}", getChannelInfo(channel), replicaOffsets.size());
    }

    public void removeReplica(SocketChannel channel) {
        if (replicaOffsets.remove(channel) != null) {
            replicationState.decrementSlaves();
            log.info("Removed replica: {}, total: {}", getChannelInfo(channel), replicaOffsets.size());
        }
    }

    public void propagateCommand(String[] commandArgs) {
        if (replicaOffsets.isEmpty())
            return;

        List<SocketChannel> failedChannels = replicaOffsets.keySet().parallelStream()
                .filter(channel -> !sendCommandToReplica(channel, commandArgs))
                .toList();

        failedChannels.forEach(this::removeReplica);
        replicationState.incrementReplicationOffset(ReplicationProtocol.calculateCommandSize(commandArgs));
    }

    public void updateReplicaOffset(SocketChannel channel, long offset) {
        AtomicLong replicaOffset = replicaOffsets.get(channel);
        if (replicaOffset != null) {
            replicaOffset.set(offset);
        }
    }

    private boolean sendCommandToReplica(SocketChannel channel, String[] commandArgs) {
        try {
            ReplicationProtocol.sendCommand(channel, commandArgs);
            return true;
        } catch (IOException e) {
            log.warn("Failed to send command to replica {}: {}", getChannelInfo(channel), e.getMessage());
            return false;
        }
    }

    private String getChannelInfo(SocketChannel channel) {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "unknown";
        }
    }
}