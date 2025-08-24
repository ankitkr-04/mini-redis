package commands.impl.strings;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;
import storage.expiry.ExpiryPolicy;

public final class SetCommand extends WriteCommand {
    @Override
    public String getName() {
        return "SET";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        if (context.getArgCount() == 3) {
            return ValidationResult.valid();
        } else if (context.getArgCount() == 5) {
            if (!"PX".equalsIgnoreCase(context.getArg(3))) {
                return ValidationResult.invalid("Only PX modifier is supported");
            }
            return CommandValidator.validateInteger(context.getArg(4));
        }
        return CommandValidator.validateArgRange(context, 3, 5);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        String value = context.getValue();

        ExpiryPolicy expiry = context.getArgCount() == 5 && "PX".equalsIgnoreCase(context.getArg(3))
                ? ExpiryPolicy.inMillis(Long.parseLong(context.getArg(4)))
                : ExpiryPolicy.never();

        context.getStorageService().setString(key, value, expiry);
        publishDataAdded(key, context.getServerContext());
        propagateCommand(context.getArgs(), context.getServerContext());

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}