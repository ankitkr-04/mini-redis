package commands.impl.strings;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.CommandValidator;
import validation.ValidationResult;

public final class GetCommand extends ReadCommand {
    @Override
    public String name() {
        return "GET";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return CommandValidator.validateArgCount(args, 2);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        var value = storage.getString(args.key());
        return new CommandResult.Success(ResponseBuilder.bulkString(value.orElse(null)));
    }
}
