package commands.impl.basic;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import config.RedisConstants;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.CommandValidator;
import validation.ValidationResult;

public final class PingCommand extends ReadCommand {
    @Override
    public String name() {
        return "PING";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return CommandValidator.validateArgCount(args, 1);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        return new CommandResult.Success(
                ResponseBuilder.encode(RedisConstants.PONG_RESPONSE));
    }
}
