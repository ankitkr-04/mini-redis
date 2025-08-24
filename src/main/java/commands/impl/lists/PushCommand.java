package commands.impl.lists;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class PushCommand extends WriteCommand {
    @Override
    public String getName() {
        return "PUSH"; // Will handle LPUSH/RPUSH
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateMinArgs(context, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        String[] values = context.getValues();
        boolean isLeft = "LPUSH".equalsIgnoreCase(context.getOperation());

        int newSize = isLeft
                ? context.getStorageService().leftPush(key, values)
                : context.getStorageService().rightPush(key, values);

        publishDataAdded(key, context.getServerContext());
        propagateCommand(context.getArgs(), context.getServerContext());

        return CommandResult.success(ResponseBuilder.integer(newSize));
    }
}