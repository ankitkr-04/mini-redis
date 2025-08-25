package commands.impl.sortedsets;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class ZCardCommand extends ReadCommand {
    @Override
    public String getName() {
        return "ZCARD";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // ZCARD key
        return CommandValidator.validateArgCount(context, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(1);
        var storage = context.getServerContext().getStorageService();

        int size = storage.zSize(key);
        return CommandResult.success(ResponseBuilder.integer(size));
    }
}
