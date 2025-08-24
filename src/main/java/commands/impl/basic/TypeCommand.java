package commands.impl.basic;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class TypeCommand extends ReadCommand {
    @Override
    public String getName() {
        return "TYPE";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var type = context.getStorageService().getType(context.getKey());
        return CommandResult.success(ResponseBuilder.simpleString(type.getDisplayName()));
    }
}