package commands.impl.replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReplicationCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;

public class PsyncCommand extends ReplicationCommand {
    private static final Logger log = LoggerFactory.getLogger(PsyncCommand.class);

    private final ReplicationHandler replicationHandler;

    public PsyncCommand() {
        this.replicationHandler = new ReplicationHandler();
    }

    @Override
    public String getName() {
        return "PSYNC";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String replId = context.getArg(1);
        String offsetStr = context.getArg(2);

        long requestedOffset = parseOffset(offsetStr);
        if (requestedOffset == Long.MIN_VALUE) {
            return CommandResult.error("Invalid offset: " + offsetStr);
        }

        return replicationHandler.handlePsync(replId, requestedOffset, context.getClientChannel(),
                context.getServerContext());
    }

    private long parseOffset(String offsetStr) {
        if ("?".equals(offsetStr)) {
            return -1;
        }

        ValidationResult validation = CommandValidator.validateInteger(offsetStr);
        if (!validation.isValid()) {
            log.warn("Invalid offset received: {}", offsetStr);
            return Long.MIN_VALUE;
        }

        return Long.parseLong(offsetStr);
    }
}