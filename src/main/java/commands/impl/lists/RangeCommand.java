package commands.impl.lists;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis LRANGE command to retrieve a sublist from a stored list.
 * <p>
 * Usage: LRANGE key start end
 * Returns the specified elements of the list stored at key.
 * </p>
 */
public final class RangeCommand extends ReadCommand {

    private static final String COMMAND_NAME = "LRANGE";
    private static final int EXPECTED_ARG_COUNT = 4;
    private static final int ARG_INDEX_START = 2;
    private static final int ARG_INDEX_END = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argCount(EXPECTED_ARG_COUNT)
                .and(CommandValidator.intArg(ARG_INDEX_START, ARG_INDEX_END)).validate(context);

    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        int startIndex = context.getIntArg(ARG_INDEX_START);
        int endIndex = context.getIntArg(ARG_INDEX_END);

        var subList = context.getStorageService().getListRange(key, startIndex, endIndex);
        return CommandResult.success(ResponseBuilder.array(subList));
    }
}
