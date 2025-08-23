package commands.impl.lists;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class LengthCommand extends ReadCommand {
    @Override
    public String name() {
        return "LLEN";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgCount(args, 2);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        return new CommandResult.Success(
                ResponseBuilder.integer(storage.getListLength(args.key())));
    }
}
