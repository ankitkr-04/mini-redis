package commands.impl.sortedsets;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Removes one or more members from a sorted set stored at the specified key.
 * Returns the number of members removed.
 * 
 * Usage: ZREM key member [member ...]
 */
public class ZRemCommand extends WriteCommand {

    private static final String COMMAND_NAME = "ZREM";
    private static final int MIN_ARGUMENTS = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Ensure at least: ZREM key member
        return CommandValidator.minArgs(MIN_ARGUMENTS).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(1);
        var storageService = context.getServerContext().getStorageService();

        int removedMembersCount = 0;
        for (int argIndex = 2; argIndex < context.getArgCount(); argIndex++) {
            String member = context.getArg(argIndex);
            if (storageService.zRemove(key, member)) {
                removedMembersCount++;
            }
        }

        return CommandResult.success(ResponseBuilder.integer(removedMembersCount));
    }
}
