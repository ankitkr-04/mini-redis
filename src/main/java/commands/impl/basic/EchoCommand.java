package commands.impl.basic;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.CommandValidator;
import validation.ValidationResult;

public final class EchoCommand extends ReadCommand {
    @Override
    public String name() {
        return "ECHO";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return CommandValidator.validateArgCount(args, 2);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        return new CommandResult.Success(ResponseBuilder.bulkString(args.arg(1)));
    }
}
