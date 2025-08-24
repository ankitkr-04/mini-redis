package commands.impl.lists;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class RangeCommand extends ReadCommand {
    @Override
    public String getName() {
        return "LRANGE";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        ValidationResult result = CommandValidator.validateArgCount(context, 4);
        if (!result.isValid())
            return result;

        result = CommandValidator.validateInteger(context.getArg(2));
        if (!result.isValid())
            return result;

        return CommandValidator.validateInteger(context.getArg(3));
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        int start = context.getIntArg(2);
        int end = context.getIntArg(3);

        var values = context.getStorageService().getListRange(key, start, end);
        return CommandResult.success(ResponseBuilder.array(values));
    }
}