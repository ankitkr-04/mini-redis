package commands.impl.strings;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class IncrCommand extends WriteCommand {
    @Override
    public String getName() {
        return "INCR";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        try {
            long newValue = context.getStorageService().incrementString(key);
            publishDataAdded(key, context.getServerContext());
            propagateCommand(context.getArgs(), context.getServerContext());

            return CommandResult.success(ResponseBuilder.integer(newValue));
        } catch (IllegalStateException | NumberFormatException e) {
            return CommandResult.error(e.getMessage());
        }
    }
}