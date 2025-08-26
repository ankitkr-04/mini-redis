package commands.impl.replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReplicationCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis WAIT command for replication.
 * <p>
 * WAIT blocks the client until the specified number of replicas have
 * acknowledged
 * the write or the timeout is reached. Returns the number of replicas that
 * acknowledged.
 * </p>
 */
public final class WaitCommand extends ReplicationCommand {
    private static final Logger log = LoggerFactory.getLogger(WaitCommand.class);

    private static final String COMMAND_NAME = "WAIT";
    private static final int EXPECTED_ARG_COUNT = 3;
    private static final int TARGET_REPLICAS_ARG_INDEX = 1;
    private static final int TIMEOUT_MILLIS_ARG_INDEX = 2;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {

        return CommandValidator.argCount(EXPECTED_ARG_COUNT).and(
                CommandValidator.intArg(TARGET_REPLICAS_ARG_INDEX, TIMEOUT_MILLIS_ARG_INDEX)).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        if (context.getServerContext().getTransactionManager().isInTransaction(context.getClientChannel())) {
            return CommandResult.error("WAIT not allowed in MULTI");
        }

        int targetReplicas = Integer.parseInt(context.getArg(TARGET_REPLICAS_ARG_INDEX));
        long timeoutMillis = Long.parseLong(context.getArg(TIMEOUT_MILLIS_ARG_INDEX));

        var replicationManager = context.getServerContext().getReplicationManager();
        var replicationState = context.getServerContext().getReplicationState();
        int connectedSlaves = replicationState.getConnectedSlaves();

        if (targetReplicas <= 0) {
            return CommandResult.success(ResponseBuilder.integer(connectedSlaves));
        }

        long masterReplicationOffset = replicationState.getMasterReplicationOffset();

        int syncedReplicas = replicationManager.getSyncReplicasCount(masterReplicationOffset);

        if (syncedReplicas >= targetReplicas || timeoutMillis == 0) {
            log.debug("WAIT: Enough replicas already synced or no timeout. Synced={}, Target={}", syncedReplicas,
                    targetReplicas);
            return CommandResult.success(ResponseBuilder.integer(syncedReplicas));
        }

        replicationManager.sendGetAckToAllReplicas();
        replicationManager.addPendingWait(context.getClientChannel(), targetReplicas, masterReplicationOffset,
                timeoutMillis);

        var timeoutScheduler = context.getServerContext().getTimeoutScheduler();
        timeoutScheduler.schedule(timeoutMillis, replicationManager::checkPendingWaits);

        log.info("WAIT: Waiting for {} replicas, timeout {} ms, offset {}", targetReplicas, timeoutMillis,
                masterReplicationOffset);

        return CommandResult.async();
    }
}
