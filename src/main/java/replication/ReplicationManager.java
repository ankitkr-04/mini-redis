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
import server.ServerContext;

/**
 * Manages master-replica synchronization and replication state.
 * Handles replica registration, command propagation, offset tracking,
 * and pending WAIT command logic.
 */
public final class ReplicationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationManager.class);

    // Command for requesting replica acknowledgements
    private static final String[] REPLCONF_GETACK_COMMAND = { "REPLCONF", "GETACK", "*" };

    // Pending WAIT request from a client
    private static record PendingWait(SocketChannel clientChannel, int requiredReplicas, long requiredOffset,
            long deadlineMillis) {
    }

    private final List<PendingWait> pendingWaits = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<SocketChannel, AtomicLong> replicaOffsets = new ConcurrentHashMap<>();
    private final ReplicationState replicationState;
    private final ServerContext serverContext;

    public ReplicationManager(ReplicationState replicationState, ServerContext serverContext) {
        this.replicationState = replicationState;
        this.serverContext = serverContext;
    }

    /**
     * Registers a new replica connection.
     * 
     * @param replicaChannel the replica's socket channel
     */
    public void addReplica(SocketChannel replicaChannel) {
        replicaOffsets.put(replicaChannel, new AtomicLong(0));
        replicationState.incrementConnectedSlaves();
        serverContext.getMetricsCollector().incrementReplicaConnections();
        LOGGER.info("Replica added: {}", getChannelInfo(replicaChannel));
    }

    /**
     * Removes a replica connection.
     * 
     * @param replicaChannel the replica's socket channel
     */
    public void removeReplica(SocketChannel replicaChannel) {
        if (replicaOffsets.remove(replicaChannel) != null) {
            replicationState.decrementConnectedSlaves();
            serverContext.getMetricsCollector().decrementReplicaConnections();
            LOGGER.info("Replica removed: {}", getChannelInfo(replicaChannel));
        }
    }

    /**
     * Checks if any replicas are connected.
     * 
     * @return true if at least one replica is connected
     */
    public boolean hasConnectedReplicas() {
        return !replicaOffsets.isEmpty();
    }

    /**
     * Returns the number of connected replicas.
     */
    public int getConnectedReplicasCount() {
        return replicaOffsets.size();
    }

    /**
     * Propagates a command to all connected replicas.
     * Removes replicas that fail to receive the command.
     * 
     * @param commandArgs the command to propagate
     */
    public void propagateCommand(String[] commandArgs) {
        if (replicaOffsets.isEmpty())
            return;

        List<SocketChannel> failedReplicas = replicaOffsets.keySet().parallelStream()
                .filter(replica -> !sendCommandToReplica(replica, commandArgs))
                .toList();

        failedReplicas.forEach(this::removeReplica);
        replicationState.incrementReplicationOffset(ReplicationProtocol.calculateCommandSize(commandArgs));
        serverContext.getMetricsCollector().incrementReplicationCommandsSent();
    }

    /**
     * Updates the replication offset for a replica.
     * 
     * @param replicaChannel the replica's socket channel
     * @param offset         the new offset
     */
    public void updateReplicaOffset(SocketChannel replicaChannel, long offset) {
        AtomicLong replicaOffset = replicaOffsets.get(replicaChannel);
        if (replicaOffset != null) {
            replicaOffset.set(offset);
        }
    }

    private boolean sendCommandToReplica(SocketChannel replicaChannel, String[] commandArgs) {
        try {
            ReplicationProtocol.sendCommand(replicaChannel, commandArgs);
            return true;
        } catch (IOException e) {
            LOGGER.debug("Failed to send command to replica {}: {}", getChannelInfo(replicaChannel), e.getMessage());
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

    /**
     * Returns the number of replicas that have acknowledged at least the given
     * offset.
     * 
     * @param requiredOffset the offset to check
     */
    public int getSyncReplicasCount(long requiredOffset) {
        return (int) replicaOffsets.values().stream()
                .filter(offset -> offset.get() >= requiredOffset)
                .count();
    }

    /**
     * Sends REPLCONF GETACK to all replicas to request acknowledgement.
     */
    public void sendGetAckToAllReplicas() {
        replicaOffsets.keySet().forEach(replica -> {
            try {
                ReplicationProtocol.sendCommand(replica, REPLCONF_GETACK_COMMAND);
            } catch (IOException e) {
                LOGGER.debug("Failed to send GETACK to replica {}: {}", getChannelInfo(replica), e.getMessage());
            }
        });
    }

    /**
     * Adds a pending WAIT request for a client.
     * 
     * @param clientChannel    the client's socket channel
     * @param requiredReplicas number of replicas to wait for
     * @param requiredOffset   offset to wait for
     * @param timeoutMillis    timeout in milliseconds
     */
    public void addPendingWait(SocketChannel clientChannel, int requiredReplicas, long requiredOffset,
            long timeoutMillis) {
        long deadlineMillis = System.currentTimeMillis() + timeoutMillis;
        pendingWaits.add(new PendingWait(clientChannel, requiredReplicas, requiredOffset, deadlineMillis));
    }

    /**
     * Removes any pending WAIT requests for a client.
     * 
     * @param clientChannel the client's socket channel
     */
    public void removePendingWait(SocketChannel clientChannel) {
        pendingWaits.removeIf(wait -> wait.clientChannel == clientChannel);
    }

    /**
     * Checks all pending WAIT requests and responds to clients if satisfied or
     * timed out.
     */
    public void checkPendingWaits() {
        long nowMillis = Instant.now().toEpochMilli();
        List<PendingWait> completedWaits = new ArrayList<>();

        for (PendingWait wait : pendingWaits) {
            boolean timedOut = nowMillis >= wait.deadlineMillis;
            int syncedReplicas = getSyncReplicasCount(wait.requiredOffset);
            boolean satisfied = syncedReplicas >= wait.requiredReplicas;

            if (timedOut || satisfied) {
                sendCurrentCount(wait.clientChannel, wait.requiredReplicas, wait.requiredOffset);
                completedWaits.add(wait);
            }
        }
        pendingWaits.removeAll(completedWaits);
    }

    /**
     * Sends the current number of synced replicas to the client.
     * 
     * @param clientChannel    the client's socket channel
     * @param requiredReplicas number of replicas requested
     * @param requiredOffset   offset requested
     */
    public void sendCurrentCount(SocketChannel clientChannel, int requiredReplicas, long requiredOffset) {
        int syncedReplicas = getSyncReplicasCount(requiredOffset);
        try {
            ReplicationProtocol.sendResponse(clientChannel, ResponseBuilder.integer(syncedReplicas));
        } catch (IOException e) {
            LOGGER.debug("Failed to send current count to client {}: {}", getChannelInfo(clientChannel),
                    e.getMessage());
        }
    }
}