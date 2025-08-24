package commands.impl.replication;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.CommandResult;
import config.ProtocolConstants;
import core.ServerContext;
import protocol.ResponseBuilder;
import server.replication.ReplicationInfo;
import server.replication.ReplicationService;

public class ReplicationHandler {
    private static final Logger log = LoggerFactory.getLogger(ReplicationHandler.class);

    private final ServerContext context;
    private final ReplicationService replicationService = new ReplicationService();

    public ReplicationHandler(ServerContext context) {
        this.context = context;
    }

    public CommandResult handlePsync(String replId, long requestedOffset, SocketChannel clientChannel) {
        ReplicationInfo replInfo = context.getServerInfo().getReplicationInfo();

        if (shouldPerformFullResync(replId, requestedOffset, replInfo)) {
            return performFullResync(replInfo, clientChannel);
        } else {
            return CommandResult.error("Partial resync not supported");
        }
    }

    private boolean shouldPerformFullResync(String replId, long requestedOffset, ReplicationInfo replInfo) {
        return "?".equals(replId) ||
                !replId.equals(replInfo.getMasterReplId()) ||
                requestedOffset == -1;
    }

    private CommandResult performFullResync(ReplicationInfo replInfo, SocketChannel clientChannel) {
        try {
            // build the FULLRESYNC header buffer
            ByteBuffer fullResyncBuf = ResponseBuilder.fullResyncBuffer(
                    replInfo.getMasterReplId(),
                    replInfo.getMasterReplOffset());

            // build the RDB bulk string buffer using byte array directly
            byte[] rdbBytes = ProtocolConstants.EMPTY_RDB_BYTES;
            ByteBuffer rdbBulk = ResponseBuilder.rdbFilePayload(rdbBytes);

            // Log for debugging
            log.debug("RDB bytes length: {}", rdbBytes.length);
            log.debug("RDB bulk buffer remaining: {}", rdbBulk.remaining());

            // combine into one buffer to ensure a single atomic write
            ByteBuffer combined = ByteBuffer.allocate(fullResyncBuf.remaining() + rdbBulk.remaining());
            combined.put(fullResyncBuf);
            combined.put(rdbBulk);
            combined.flip();

            // use the server.replication.ReplicationService that has
            // sendResponse(SocketChannel, ByteBuffer)
            replicationService.sendResponse(clientChannel, combined);

            // add replica to manager after successful send
            context.getReplicationManager().addReplica(clientChannel);

            log.info("Full resync completed for new replica");
            return CommandResult.async();
        } catch (Exception e) {
            log.error("Full resync failed", e);
            return CommandResult.error("Full resync failed");
        }
    }

}