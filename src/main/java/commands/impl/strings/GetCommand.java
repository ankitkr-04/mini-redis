package commands.impl.strings;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class GetCommand extends ReadCommand {
    @Override
    public String getName() {
        return "GET";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var value = context.getStorageService().getString(context.getKey());
        return CommandResult.success(ResponseBuilder.bulkString(value.orElse(null)));
    }
}