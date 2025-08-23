package commands.impl.lists;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.CommandValidator;
import validation.ValidationResult;

public final class RangeCommand extends ReadCommand {
    @Override
    public String name() {
        return "LRANGE";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        var result = CommandValidator.validateArgCount(args, 4);
        if (!result.isValid())
            return result;

        result = CommandValidator.validateInteger(args.arg(2));
        if (!result.isValid())
            return result;

        return CommandValidator.validateInteger(args.arg(3));
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String key = args.key();
        int start = Integer.parseInt(args.arg(2));
        int end = Integer.parseInt(args.arg(3));

        var values = storage.getListRange(key, start, end);
        return new CommandResult.Success(ResponseBuilder.array(values));
    }
}
