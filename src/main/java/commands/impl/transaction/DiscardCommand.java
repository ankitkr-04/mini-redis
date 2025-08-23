package commands.impl.transaction;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.WriteCommand;
import config.RedisConstants;
import errors.ErrorCode;
import events.StorageEventPublisher;
import protocol.ResponseBuilder;
import storage.StorageService;
import transaction.TransactionManager;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class DiscardCommand extends WriteCommand {
    private final TransactionManager transactionManager;

    public DiscardCommand(TransactionManager manager, StorageEventPublisher eventPublisher) {
        super(eventPublisher);
        this.transactionManager = manager;
    }

    @Override
    public String name() {
        return "DISCARD";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgCount(args, 1);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        var state = transactionManager.getOrCreateState(args.clientChannel());

        if (!state.isInTransaction()) {
            return new CommandResult.Error(ErrorCode.DISCARD_WITHOUT_MULTI.getMessage());
        }

        state.clearTransaction();
        return new CommandResult.Success(ResponseBuilder.encode(RedisConstants.OK_RESPONSE));
    }

}
