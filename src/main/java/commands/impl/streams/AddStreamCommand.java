package commands.impl.streams;

import java.util.Map;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import errors.ErrorCode;
import protocol.ResponseBuilder;
import storage.expiry.ExpiryPolicy;

public final class AddStreamCommand extends WriteCommand {
    @Override
    public String getName() {
        return "XADD";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        ValidationResult res = CommandValidator.validateMinArgs(context, 4);
        if (!res.isValid())
            return res;

        if ((context.getArgCount() - 3) % 2 != 0) {
            return ValidationResult.invalid(ErrorCode.WRONG_ARG_COUNT.getMessage());
        }

        return CommandValidator.validateStreamId(context.getArg(2));
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        String id = context.getArg(2);
        Map<String, String> fields = context.getFieldValueMap(3);

        try {
            String entryId = context.getStorageService().addStreamEntry(key, id, fields, ExpiryPolicy.never());
            publishDataAdded(key, context.getServerContext());
            propagateCommand(context.getArgs(), context.getServerContext());

            return CommandResult.success(ResponseBuilder.bulkString(entryId));
        } catch (IllegalArgumentException e) {
            return CommandResult.error(e.getMessage());
        }
    }
}