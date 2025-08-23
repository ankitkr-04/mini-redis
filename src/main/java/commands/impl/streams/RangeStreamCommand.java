package commands.impl.streams;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.ReadCommand;
import errors.ErrorCode;
import errors.ServerError;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class RangeStreamCommand extends ReadCommand {

    @Override
    public String name() {
        return "XRANGE";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        if (args.argCount() != 4 && args.argCount() != 6) {
            return ValidationResult.invalid(ServerError.validation(
                    ErrorCode.WRONG_ARG_COUNT.getMessage()));
        }
        if (args.argCount() == 6) {
            if (!"COUNT".equalsIgnoreCase(args.arg(4))) {
                return ValidationResult.invalid(ServerError.validation(
                        ErrorCode.WRONG_ARG_COUNT.getMessage()));
            }
            return ValidationUtils.validateInteger(args.arg(5));
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
