package server.replication;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ProtocolConstants;

public class ReplicationManager {
    private static final Logger log = LoggerFactory.getLogger(ReplicationManager.class);

    private final ConcurrentHashMap<SocketChannel, AtomicLong> replicas = new ConcurrentHashMap<>();
    private final ReplicationInfo replicationInfo;
    private final ReplicationService replicationService = new ReplicationService();

    public ReplicationManager(ReplicationInfo replicationInfo) {
        this.replicationInfo = replicationInfo;
    }

    public void addReplica(SocketChannel channel) {
        replicas.put(channel, new AtomicLong(0));
        replicationInfo.incrementSlaves();
        log.info("Added replica: {}, total: {}", getChannelInfo(channel), replicas.size());
    }

    public void removeReplica(SocketChannel channel) {
        if (replicas.remove(channel) != null) {
            replicationInfo.decrementSlaves();
            log.info("Removed replica: {}, total: {}", getChannelInfo(channel), replicas.size());
        }
    }

    public void propagateCommand(String[] commandArgs) {
        if (replicas.isEmpty())
            return;

        List<SocketChannel> failedChannels = replicas.keySet().parallelStream()
                .filter(channel -> !sendToReplica(channel, commandArgs))
                .toList();

        failedChannels.forEach(this::removeReplica);
        replicationInfo.incrementReplOffset(calculateCommandSize(commandArgs));
    }

    private boolean sendToReplica(SocketChannel channel, String[] commandArgs) {
        try {
            replicationService.sendCommand(channel, commandArgs);
            return true;
        } catch (IOException e) {
            log.warn("Failed to send to replica {}: {}", getChannelInfo(channel), e.getMessage());
            return false;
        }
    }

    public void updateReplicaOffset(SocketChannel channel, long offset) {
        AtomicLong replicaOffset = replicas.get(channel);
        if (replicaOffset != null) {
            replicaOffset.set(offset);
        }
    }

    private String getChannelInfo(SocketChannel channel) {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
            return "unknown";
        }
    }

    private long calculateCommandSize(String[] commandArgs) {
        long size = String.valueOf(ProtocolConstants.ARRAY).length() +
                String.valueOf(commandArgs.length).length() +
                ProtocolConstants.CRLF.length();

        for (String arg : commandArgs) {
            size += String.valueOf(ProtocolConstants.BULK_STRING).length() +
                    String.valueOf(arg.length()).length() +
                    ProtocolConstants.CRLF.length() +
                    arg.length() +
                    ProtocolConstants.CRLF.length();
        }
        return size;
    }
}