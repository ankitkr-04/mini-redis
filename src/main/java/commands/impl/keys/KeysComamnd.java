package commands.impl.keys;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class KeysComamnd extends ReadCommand {
    @Override
    public String getName() {
        return "KEYS";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var pattern = context.getArg(1);
        var keys = context.getStorageService().getKeysByPattern(pattern);

        return CommandResult.success(ResponseBuilder.array(keys));
    }

}
