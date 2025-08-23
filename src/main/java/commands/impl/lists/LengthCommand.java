package commands.impl.lists;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.CommandValidator;
import validation.ValidationResult;

public final class LengthCommand extends ReadCommand {
    @Override
    public String name() {
        return "LLEN";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return CommandValidator.validateArgCount(args, 2);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        int length = storage.getListLength(args.key());
        return new CommandResult.Success(ResponseBuilder.integer(length));
    }
}
