package commands.impl.transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import protocol.ResponseBuilder;

public final class ExecCommand extends WriteCommand {
    @Override
    public String getName() {
        return "EXEC";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 1);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var transactionManager = context.getServerContext().getTransactionManager();
        var state = transactionManager.getOrCreateState(context.getClientChannel());

        if (!state.isInTransaction())
            return CommandResult.error(ErrorCode.EXEC_WITHOUT_MULTI.getMessage());

        // Check if any watched keys have been modified (optimistic locking)
        if (state.isTransactionInvalid()) {
            state.clearTransaction();
            state.clearWatchedKeys();
            return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_NULL_ARRAY));
        }

        var queuedCommands = state.getQueuedCommands();
        state.clearTransaction();
        state.clearWatchedKeys(); // Clear watched keys after successful execution

        if (queuedCommands.isEmpty())
            return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_EMPTY_ARRAY));

        var results = new ArrayList<ByteBuffer>();
        for (var queued : queuedCommands) {
            CommandResult result = queued.command().execute(new CommandContext(queued.operation(), queued.rawArgs(),
                    context.getClientChannel(), context.getStorageService(), context.getServerContext()));
            results.add(switch (result) {
                case CommandResult.Success s -> s.response();
                case CommandResult.Error e -> ResponseBuilder.error(e.message());
                case CommandResult.Async _ -> ResponseBuilder
                        .error(ErrorCode.BLOCKING_IN_TRANSACTION.getMessage());
            });
        }
        return CommandResult.success(ResponseBuilder.arrayOfBuffers(results));
    }
}