package commands.impl.basic;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class TypeCommand extends ReadCommand {
    @Override
    public String name() {
        return "TYPE";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgCount(args, 2);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        var type = storage.getType(args.key());
        return new CommandResult.Success(ResponseBuilder.simpleString(type.getDisplayName()));
    }
}
