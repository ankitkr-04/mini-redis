package commands.impl.lists;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis LLEN command to return the length of a list stored at a
 * given key.
 * <p>
 * Usage: LLEN key
 * Returns the length of the list stored at the specified key.
 * </p>
 */
public final class LengthCommand extends ReadCommand {

    private static final String COMMAND_NAME = "LLEN";
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
        int listLength = context.getStorageService().getListLength(context.getKey());
        return CommandResult.success(ResponseBuilder.integer(listLength));
    }
}
