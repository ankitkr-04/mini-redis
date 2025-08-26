package commands.impl.basic;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis ECHO command.
 * <p>
 * Returns the given string as a bulk string reply.
 * </p>
 */
public final class EchoCommand extends ReadCommand {

    private static final String COMMAND_NAME = "ECHO";
    private static final int EXPECTED_ARG_COUNT = 2;

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
        return CommandResult.success(ResponseBuilder.bulkString(context.getArg(1)));
    }
}
