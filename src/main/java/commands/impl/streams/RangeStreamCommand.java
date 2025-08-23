package commands.impl.streams;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import errors.ErrorCode;
import errors.ValidationError;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.CommandValidator;
import validation.ValidationResult;

public final class RangeStreamCommand extends ReadCommand {

    @Override
    public String name() {
        return "XRANGE";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        if (args.argCount() != 4 && args.argCount() != 6) {
            return ValidationResult.invalid(new ValidationError(
                    ErrorCode.WRONG_ARG_COUNT.getMessage(),
                    ErrorCode.WRONG_ARG_COUNT));
        }
        if (args.argCount() == 6) {
            if (!"COUNT".equalsIgnoreCase(args.arg(4))) {
                return ValidationResult.invalid(new ValidationError(
                        "Expected COUNT",
                        ErrorCode.WRONG_ARG_COUNT));
            }
            return CommandValidator.validateInteger(args.arg(5));
        }
        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        int count = args.argCount() == 4 ? 0 : Integer.parseInt(args.arg(5));
        var res = storage.getStreamRange(args.key(), args.arg(2), args.arg(3), count);
        return new CommandResult.Success(
                ResponseBuilder.streamEntries(res, e -> e.id(), e -> e.fieldList()));
    }
}
