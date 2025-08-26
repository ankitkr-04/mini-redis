package commands.impl.replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReplicationCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;

/**
 * Handles the PSYNC command for Redis replication.
 * <p>
 * This command is used by replica clients to synchronize with the master.
 * It expects three arguments: the command name, replication ID, and offset.
 * </p>
 */
public class PsyncCommand extends ReplicationCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsyncCommand.class);

    private static final String COMMAND_NAME = "PSYNC";
    private static final int EXPECTED_ARG_COUNT = 3;
    private static final int REPL_ID_ARG_INDEX = 1;
    private static final int OFFSET_ARG_INDEX = 2;
    private static final long UNKNOWN_OFFSET = -1L;
    private static final long INVALID_OFFSET = Long.MIN_VALUE;

    private final ReplicationHandler replicationHandler;

    public PsyncCommand() {
        this.replicationHandler = new ReplicationHandler();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argCount(EXPECTED_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String replicationId = context.getArg(REPL_ID_ARG_INDEX);
        String offsetArgument = context.getArg(OFFSET_ARG_INDEX);

        long offset = parseOffset(offsetArgument);
        if (offset == INVALID_OFFSET) {
            return CommandResult.error("Invalid offset: " + offsetArgument);
        }

        LOGGER.debug("Processing PSYNC with replicationId={} and offset={}", replicationId, offset);

        return replicationHandler.handlePsync(
                replicationId,
                offset,
                context.getClientChannel(),
                context.getServerContext());
    }

    /**
     * Parses the offset argument for the PSYNC command.
     * Returns UNKNOWN_OFFSET if the argument is "?".
     * Returns INVALID_OFFSET if the argument is not a valid integer.
     *
     * @param offsetArgument the offset argument as a string
     * @return the parsed offset as a long
     */
    private long parseOffset(String offsetArgument) {
        if ("?".equals(offsetArgument)) {
            return UNKNOWN_OFFSET;
        }

        ValidationResult validation = CommandValidator.validateInteger(offsetArgument);
        if (!validation.isValid()) {
            LOGGER.info("Invalid offset received: {}", offsetArgument);
            return INVALID_OFFSET;
        }

        return Long.parseLong(offsetArgument);
    }
}