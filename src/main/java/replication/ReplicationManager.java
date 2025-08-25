package replication;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protocol.ResponseBuilder;

public final class ReplicationManager {
    private static final Logger log = LoggerFactory.getLogger(ReplicationManager.class);

    private static record PendingWait(SocketChannel channel, int targetReplicas, long targetOffset, long endTime) {
    }

    private final List<PendingWait> pendingWaits = new CopyOnWriteArrayList<>();

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
            long oldOffset = replicaOffset.get();
            replicaOffset.set(offset);
            log.info("Updated replica offset for {}: {} -> {}", getChannelInfo(channel), oldOffset, offset);
        } else {
            log.warn("No replica offset found for channel: {}", getChannelInfo(channel));
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

    // Wait for replication to specified number of replicas at given offset or
    // timeout
    public int getSyncReplicasCount(long targetOffset) {
        int count = (int) replicaOffsets.values().stream()
                .filter(offset -> offset.get() >= targetOffset)
                .count();
        log.info("getSyncReplicasCount({}): {} replicas synced. Current offsets: {}",
                targetOffset, count,
                replicaOffsets.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> getChannelInfo(e.getKey()),
                                e -> e.getValue().get())));
        return count;
    }

    public void sendGetAckToAllReplicas() {
        String[] command = new String[] { "REPLCONF", "GETACK", "*" };
        replicaOffsets.keySet().forEach(channel -> {
            try {
                ReplicationProtocol.sendCommand(channel, command);
            } catch (IOException e) {
                log.warn("Failed to send GETACK to replica {}: {}", getChannelInfo(channel), e.getMessage());
            }
        });
    }

    public void addPendingWait(SocketChannel channel, int targetReplicas, long targetOffset, long timeoutMillis) {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        pendingWaits.add(new PendingWait(channel, targetReplicas, targetOffset, endTime));
    }

    public void reemovePendingWait(SocketChannel client) {
        pendingWaits.removeIf(pw -> pw.channel == client);
    }

    public void checkPendingWaits() {
        long now = Instant.now().toEpochMilli();
        List<PendingWait> toRemove = new ArrayList<>();

        for (PendingWait pw : pendingWaits) {
            if (now >= pw.endTime) {
                log.info("Pending wait timed out for channel: {}", getChannelInfo(pw.channel));
                sendCurrentCount(pw.channel, pw.targetReplicas, pw.targetOffset);
                toRemove.add(pw);
                continue;
            }

            int syncedReplicas = getSyncReplicasCount(pw.targetOffset);
            if (syncedReplicas >= pw.targetReplicas) {
                log.info("Pending wait satisfied for channel: {}, synced replicas: {}",
                        getChannelInfo(pw.channel), syncedReplicas);
                sendCurrentCount(pw.channel, pw.targetReplicas, pw.targetOffset);
                toRemove.add(pw);
            }
        }

        // Remove completed waits
        pendingWaits.removeAll(toRemove);
    }

    public void sendCurrentCount(SocketChannel channel, int targetReplicas, long targetOffset) {
        int syncedReplicas = getSyncReplicasCount(targetOffset);
        try {
            ReplicationProtocol.sendResponse(channel, ResponseBuilder.integer(syncedReplicas));
        } catch (IOException e) {
            log.warn("Failed to send current count to channel {}: {}", getChannelInfo(channel), e.getMessage());
        }
    }

}