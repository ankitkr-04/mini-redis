package commands.impl.transaction;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.WriteCommand;
import config.ProtocolConstants;
import errors.ErrorCode;
import events.StorageEventPublisher;
import protocol.ResponseBuilder;
import storage.StorageService;
import transaction.TransactionManager;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class MultiCommand extends WriteCommand {
    private final TransactionManager transactionManager;

    public MultiCommand(TransactionManager manager, StorageEventPublisher eventPublisher) {
        super(eventPublisher);
        this.transactionManager = manager;

    }

    @Override
    public String name() {
        return "MULTI";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgCount(args, 1);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        var state = transactionManager.getOrCreateState(args.clientChannel());

        if (state.isInTransaction()) {
            return new CommandResult.Error(ErrorCode.NESTED_MULTI.getMessage());
        }

        state.beginTransaction();
        return new CommandResult.Success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }

}
