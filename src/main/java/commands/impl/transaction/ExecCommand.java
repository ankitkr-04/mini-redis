package commands.impl.transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;

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

public final class ExecCommand extends WriteCommand {
    private final TransactionManager transactionManager;

    public ExecCommand(TransactionManager manager, StorageEventPublisher eventPublisher) {
        super(eventPublisher);
        this.transactionManager = manager;
    }

    @Override
    public String name() {
        return "EXEC";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgCount(args, 1);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        var state = transactionManager.getOrCreateState(args.clientChannel());
        if (!state.isInTransaction())
            return new CommandResult.Error(ErrorCode.EXEC_WITHOUT_MULTI.getMessage());
        var queuedCommands = state.getQueuedCommands();
        state.clearTransaction();
        if (queuedCommands.isEmpty())
            return new CommandResult.Success(ResponseBuilder.encode(ProtocolConstants.RESP_EMPTY_ARRAY));
        var results = new ArrayList<ByteBuffer>();
        for (var queued : queuedCommands) {
            CommandResult result = queued.command().execute(queued.args(), storage);
            results.add(switch (result) {
                case CommandResult.Success s -> s.response();
                case CommandResult.Error e -> ResponseBuilder.error(e.message());
                case CommandResult.Async _ -> ResponseBuilder
                        .error(ErrorCode.BLOCKING_IN_TRANSACTION.getMessage());
            });
        }
        return new CommandResult.Success(ResponseBuilder.arrayOfBuffers(results));
    }
}
