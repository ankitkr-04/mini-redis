package commands.impl.strings;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.WriteCommand;
import config.ProtocolConstants;
import errors.ErrorCode;
import errors.ServerError;
import events.StorageEventPublisher;
import protocol.ResponseBuilder;
import storage.StorageService;
import storage.expiry.ExpiryPolicy;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class SetCommand extends WriteCommand {
    public SetCommand(StorageEventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public String name() {
        return "SET";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        if (args.argCount() == 3) {
            return ValidationResult.valid();
        } else if (args.argCount() == 5) {
            if (!"PX".equalsIgnoreCase(args.arg(3))) {
                return ValidationResult.invalid(
                        ServerError.validation(ErrorCode.WRONG_ARG_COUNT.getMessage()));
            }
            return ValidationUtils.validateInteger(args.arg(4));
        }
        return ValidationUtils.validateArgRange(args, 3, 5);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String key = args.key();
        String value = args.value();

        ExpiryPolicy expiry = args.argCount() == 5 && "PX".equalsIgnoreCase(args.arg(3))
                ? ExpiryPolicy.inMillis(Long.parseLong(args.arg(4)))
                : ExpiryPolicy.never();

        storage.setString(key, value, expiry);
        publishDataAdded(key);

        // Propagate to replicas
        propagateCommand(args.rawArgs());

        return new CommandResult.Success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}