package commands.impl.basic;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import config.ProtocolConstants;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class PingCommand extends ReadCommand {
    @Override
    public String name() {
        return "PING";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgCount(args, 1);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        return new CommandResult.Success(
                ResponseBuilder.encode(ProtocolConstants.RESP_PONG));
    }
}
