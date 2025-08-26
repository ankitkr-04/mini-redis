package commands.impl.transaction;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

/**
 * Implements the Redis UNWATCH command, which clears all watched keys for the
 * current client.
 */
public final class UnwatchCommand extends WriteCommand {

    /** The name of this command. */
    private static final String COMMAND_NAME = "UNWATCH";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // UNWATCH does not require any arguments or validation.
        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        // Remove all watched keys for the client associated with this context.
        context.getServerContext()
                .getTransactionManager()
                .unwatchAllKeys(context.getClientChannel());

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}
