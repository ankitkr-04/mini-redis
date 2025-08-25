package commands.impl.replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReplicationCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class WaitCommand extends ReplicationCommand {
    private static final Logger log = LoggerFactory.getLogger(WaitCommand.class);

    @Override
    public String getName() {
        return "WAIT";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        if (!CommandValidator.validateArgCount(context, 3).isValid() ||
                !CommandValidator.validateInteger(context.getArg(1)).isValid() ||
                !CommandValidator.validateInteger(context.getArg(2)).isValid()) {
            return ValidationResult.invalid("wrong number of arguments for 'wait' command");
        }
        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        if (context.getServerContext().getTransactionManager().isInTransaction(context.getClientChannel()))
            return CommandResult.error("WAIT not allowed in MULTI");

        int targetReplicas = Integer.parseInt(context.getArg(1));
        long timeoutMillis = Long.parseLong(context.getArg(2));

        var mgr = context.getServerContext().getReplicationManager();
        var state = context.getServerContext().getReplicationState();
        var connectedSlaves = state.getConnectedSlaves();

        if (targetReplicas <= 0) {
            return CommandResult.success(ResponseBuilder.integer(connectedSlaves));
        }

        long targetOffset = state.getMasterReplicationOffset();
        log.info("WAIT command: targetReplicas={}, timeoutMillis={}, targetOffset={}",
                targetReplicas, timeoutMillis, targetOffset);

        int syncedReplicas = mgr.getSyncReplicasCount(targetOffset);
        log.debug("Initial synced replicas: {}", syncedReplicas);

        if (syncedReplicas >= targetReplicas || timeoutMillis == 0) {
            log.debug("Returning immediately: synced={}, target={}", syncedReplicas, targetReplicas);
            return CommandResult.success(ResponseBuilder.integer(syncedReplicas));
        }

        log.debug("Sending GETACK to all replicas");
        mgr.sendGetAckToAllReplicas();

        log.debug("Adding pending wait and scheduling timeout");
        mgr.addPendingWait(context.getClientChannel(), targetReplicas, targetOffset, timeoutMillis);

        // Add to Scheduler
        var scheduler = context.getServerContext().getTimeoutScheduler();
        scheduler.schedule(timeoutMillis, () -> {
            log.debug("WAIT timeout triggered, checking pending waits");
            mgr.checkPendingWaits();
        });

        return CommandResult.async();
    }

}
