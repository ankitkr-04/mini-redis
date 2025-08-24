package commands.impl.replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.BaseCommand;
import core.ServerContext;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public class PsyncCommand extends BaseCommand {
    private static final Logger log = LoggerFactory.getLogger(PsyncCommand.class);

    private final ServerContext context;
    private final ReplicationHandler replicationHandler;

    public PsyncCommand(ServerContext context) {
        this.context = context;
        this.replicationHandler = new ReplicationHandler(context);
    }

    @Override
    public String name() {
        return "PSYNC";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgCount(args, 3);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String replId = args.arg(1);
        String offsetStr = args.arg(2);

        long requestedOffset = parseOffset(offsetStr);
        if (requestedOffset == Long.MIN_VALUE) {
            return CommandResult.error("Invalid offset: " + offsetStr);
        }

        return replicationHandler.handlePsync(replId, requestedOffset, args.clientChannel());
    }

    private long parseOffset(String offsetStr) {
        if ("?".equals(offsetStr)) {
            return -1;
        }

        ValidationResult validation = ValidationUtils.validateInteger(offsetStr);
        if (!validation.isValid()) {
            log.warn("Invalid offset received: {}", offsetStr);
            return Long.MIN_VALUE;
        }

        return Long.parseLong(offsetStr);
    }
}