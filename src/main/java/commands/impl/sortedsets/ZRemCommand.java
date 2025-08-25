package commands.impl.sortedsets;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class ZRemCommand extends WriteCommand {

    @Override
    public String getName() {
        return "ZREM";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // ZREM key member [member ...]
        return CommandValidator.validateMinArgs(context, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(1);
        var storage = context.getServerContext().getStorageService();

        int removedCount = 0;
        for (int i = 2; i < context.getArgCount(); i++) {
            String member = context.getArg(i);
            if (storage.zRemove(key, member)) {
                removedCount++;
            }
        }

        return CommandResult.success(ResponseBuilder.integer(removedCount));
    }

}
