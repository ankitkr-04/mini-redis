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

/**
 * Handles replication commands for Redis protocol, specifically the PSYNC
 * command.
 * Determines whether a full or partial resynchronization is required and
 * performs the appropriate action.
 */
public class ReplicationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicationHandler.class);

    // Constant for unknown replication id as per Redis protocol
    private static final String UNKNOWN_REPL_ID = "?";
    // Constant for requested offset indicating full resync
    private static final long FULL_RESYNC_OFFSET = -1;

    /**
     * Handles the PSYNC command from a replica.
     *
     * @param replicationId   The replication id sent by the replica.
     * @param requestedOffset The replication offset requested by the replica.
     * @param replicaChannel  The channel to the replica.
     * @param serverContext   The server context.
     * @return CommandResult indicating the outcome.
     */
    public CommandResult handlePsync(String replicationId, long requestedOffset, SocketChannel replicaChannel,
            ServerContext serverContext) {
        ReplicationState replicationState = serverContext.getReplicationState();

        if (requiresFullResync(replicationId, requestedOffset, replicationState)) {
            return performFullResync(replicationState, replicaChannel, serverContext);
        } else {
            return CommandResult.error("Partial resync not supported");
        }
    }

    /**
     * Determines if a full resynchronization is required based on the replication
     * id and offset.
     *
     * @param replicationId    The replication id sent by the replica.
     * @param requestedOffset  The replication offset requested by the replica.
     * @param replicationState The current replication state of the server.
     * @return True if a full resync is required, false otherwise.
     */
    private boolean requiresFullResync(String replicationId, long requestedOffset, ReplicationState replicationState) {
        return UNKNOWN_REPL_ID.equals(replicationId)
                || !replicationId.equals(replicationState.getMasterReplicationId())
                || requestedOffset == FULL_RESYNC_OFFSET;
    }

    /**
     * Performs a full resynchronization by sending the FULLRESYNC header and an
     * empty RDB file to the replica.
     *
     * @param replicationState The current replication state of the server.
     * @param replicaChannel   The channel to the replica.
     * @param serverContext    The server context.
     * @return CommandResult indicating the outcome.
     */
    private CommandResult performFullResync(ReplicationState replicationState, SocketChannel replicaChannel,
            ServerContext serverContext) {
        try {
            ByteBuffer fullResyncHeader = ResponseBuilder.fullResyncResponse(
                    replicationState.getMasterReplicationId(),
                    replicationState.getMasterReplicationOffset());

            byte[] emptyRdbBytes = ProtocolConstants.EMPTY_RDB_BYTES;
            ByteBuffer rdbPayload = ResponseBuilder.rdbFilePayload(emptyRdbBytes);

            ByteBuffer combinedResponse = ByteBuffer.allocate(fullResyncHeader.remaining() + rdbPayload.remaining());
            combinedResponse.put(fullResyncHeader);
            combinedResponse.put(rdbPayload);
            combinedResponse.flip();

            ReplicationProtocol.sendResponse(replicaChannel, combinedResponse);

            serverContext.getReplicationManager().addReplica(replicaChannel);

            LOGGER.info("Full resync completed for new replica at offset {}",
                    replicationState.getMasterReplicationOffset());
            return CommandResult.async();
        } catch (Exception e) {
            LOGGER.error("Full resync failed", e);
            return CommandResult.error("Full resync failed");
        }
    }
}