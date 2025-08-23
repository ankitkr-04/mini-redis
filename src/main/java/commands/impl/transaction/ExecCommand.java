package commands.impl.transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import commands.CommandArgs;
import commands.CommandResult;
import commands.base.WriteCommand;
import config.RedisConstants;
import errors.ErrorCode;
import events.StorageEventPublisher;
import protocol.ResponseBuilder;
import storage.StorageService;
import transaction.TransactionManager;
import validation.CommandValidator;
import validation.ValidationResult;

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
    protected ValidationResult validateCommand(commands.CommandArgs args) {
        return CommandValidator.validateArgCount(args, 1);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {

        var state = transactionManager.getOrCreateState(args.clientChannel());

        if (!state.isInTransaction()) {
            return new CommandResult.Error(errors.ErrorCode.EXEC_WITHOUT_MULTI.getMessage());
        }

        var queuedCommands = state.getQueuedCommands();
        state.clearTransaction();

        if (queuedCommands.isEmpty()) {
            return new CommandResult.Success(ResponseBuilder.encode(RedisConstants.EMPTY_ARRAY));
        }

        var results = new ArrayList<ByteBuffer>();
        for (var queued : queuedCommands) {
            try {
                var result = queued.command().execute(args, storage);
                if (result instanceof CommandResult.Success success) {
                    results.add(success.response());
                } else if (result instanceof CommandResult.Error error) {
                    results.add(ResponseBuilder.error(error.message()));
                } else {
                    results.add(
                            ResponseBuilder.error(ErrorCode.BLOCKNG_IN_TRANSACTION.getMessage()));
                }
            } catch (Exception e) {
                results.add(ResponseBuilder.error(e.getMessage()));

            }
        }


        return new CommandResult.Success(ResponseBuilder.arrayOfBuffers(results));
    }


}
