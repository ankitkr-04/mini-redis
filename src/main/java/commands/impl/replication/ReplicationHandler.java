package commands.impl.replication;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.result.CommandResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;
import replication.ReplicationProtocol;
import replication.ReplicationState;
import server.ServerContext;

public class ReplicationHandler {
    private static final Logger log = LoggerFactory.getLogger(ReplicationHandler.class);

    public CommandResult handlePsync(String replId, long requestedOffset, SocketChannel clientChannel,
            ServerContext context) {
        var replState = context.getReplicationState();

        if (shouldPerformFullResync(replId, requestedOffset, replState)) {
            return performFullResync(replState, clientChannel, context);
        } else {
            return CommandResult.error("Partial resync not supported");
        }
    }

    private boolean shouldPerformFullResync(String replId, long requestedOffset, ReplicationState replState) {
        return "?".equals(replId) ||
                !replId.equals(replState.getMasterReplicationId()) ||
                requestedOffset == -1;
    }

    private CommandResult performFullResync(ReplicationState replState, SocketChannel clientChannel,
            ServerContext context) {
        try {
            // Build the FULLRESYNC header buffer
            ByteBuffer fullResyncBuf = ResponseBuilder.fullResyncResponse(
                    replState.getMasterReplicationId(),
                    replState.getMasterReplicationOffset());

            // Build the RDB bulk string buffer using byte array directly
            byte[] rdbBytes = ProtocolConstants.EMPTY_RDB_BYTES;
            ByteBuffer rdbBulk = ResponseBuilder.rdbFilePayload(rdbBytes);

            // Log for debugging
            log.debug("RDB bytes length: {}", rdbBytes.length);
            log.debug("RDB bulk buffer remaining: {}", rdbBulk.remaining());

            // Combine into one buffer to ensure a single atomic write
            ByteBuffer combined = ByteBuffer.allocate(fullResyncBuf.remaining() + rdbBulk.remaining());
            combined.put(fullResyncBuf);
            combined.put(rdbBulk);
            combined.flip();

            // Use ReplicationProtocol to send response
            ReplicationProtocol.sendResponse(clientChannel, combined);

            // Add replica to manager after successful send
            context.getReplicationManager().addReplica(clientChannel);

            log.info("Full resync completed for new replica");
            return CommandResult.async();
        } catch (Exception e) {
            log.error("Full resync failed", e);
            return CommandResult.error("Full resync failed");
        }
    }
}