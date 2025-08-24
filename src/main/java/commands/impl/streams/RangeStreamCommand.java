package commands.impl.streams;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import errors.ErrorCode;
import protocol.ResponseBuilder;

public final class RangeStreamCommand extends ReadCommand {
    @Override
    public String getName() {
        return "XRANGE";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        if (context.getArgCount() != 4 && context.getArgCount() != 6) {
            return ValidationResult.invalid(ErrorCode.WRONG_ARG_COUNT.getMessage());
        }
        if (context.getArgCount() == 6) {
            if (!"COUNT".equalsIgnoreCase(context.getArg(4))) {
                return ValidationResult.invalid(ErrorCode.WRONG_ARG_COUNT.getMessage());
            }
            return CommandValidator.validateInteger(context.getArg(5));
        }
        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        int count = context.getArgCount() == 4 ? 0 : context.getIntArg(5);
        var res = context.getStorageService().getStreamRange(context.getKey(), context.getArg(2), context.getArg(3),
                count);
        return CommandResult.success(
                ResponseBuilder.streamEntries(res, e -> e.id(), e -> e.fieldList()));
    }
}