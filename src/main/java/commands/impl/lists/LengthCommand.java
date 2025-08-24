package commands.impl.lists;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class LengthCommand extends ReadCommand {
    @Override
    public String getName() {
        return "LLEN";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        return CommandResult.success(
                ResponseBuilder.integer(context.getStorageService().getListLength(context.getKey())));
    }
}