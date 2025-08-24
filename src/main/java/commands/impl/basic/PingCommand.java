package commands.impl.basic;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

public final class PingCommand extends ReadCommand {
    @Override
    public String getName() {
        return "PING";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 1);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_PONG));
    }
}