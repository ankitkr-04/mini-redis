package commands.impl.basic;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class EchoCommand extends ReadCommand {
    @Override
    public String getName() {
        return "ECHO";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        return CommandResult.success(ResponseBuilder.bulkString(context.getArg(1)));
    }
}