package commands.impl.replication;

import commands.base.ReplicationCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class WaitCommand extends ReplicationCommand {
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
        int syncedReplicas = mgr.getSyncReplicasCount(targetOffset);
        if (syncedReplicas >= targetReplicas || timeoutMillis == 0) {
            return CommandResult.success(ResponseBuilder.integer(syncedReplicas));
        }

        mgr.sendGetAckToAllReplicas();

        mgr.addPendingWait(context.getClientChannel(), targetReplicas, targetOffset, timeoutMillis);

        // Add to Scheduler
        var scheduler = context.getServerContext().getTimeoutScheduler();
        scheduler.schedule(timeoutMillis, () -> {
            mgr.checkPendingWaits();
        });

        return CommandResult.async();
    }

}
