package commands.impl.streams;

import java.util.Map;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.WriteCommand;
import errors.ErrorCode;
import errors.ServerError;
import events.StorageEventPublisher;
import protocol.ResponseBuilder;
import storage.StorageService;
import storage.expiry.ExpiryPolicy;
import validation.ValidationResult;
import validation.ValidationUtils;

public class AddStreamCommand extends WriteCommand {

    public AddStreamCommand(StorageEventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public String name() {
        return "XADD";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        var res = ValidationUtils.validateArgRange(args, 4, Integer.MAX_VALUE);
        if (!res.isValid())
            return res;

        if ((args.argCount() - 3) % 2 != 0) {
            return ValidationResult.invalid(ServerError.validation(ErrorCode.WRONG_ARG_COUNT.getMessage()));
        }

        return ValidationUtils.validateStreamId(args.arg(2));
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String key = args.key();
        String id = args.arg(2);
        Map<String, String> fields = args.fieldValueMap(3);

        try {
            String entryId = storage.addStreamEntry(key, id, fields, ExpiryPolicy.never());
            publishDataAdded(key);

            // Propagate to replicas
            propagateCommand(args.rawArgs());

            return new CommandResult.Success(ResponseBuilder.bulkString(entryId));
        } catch (IllegalArgumentException e) {
            return new CommandResult.Error(e.getMessage());
        }
    }
}