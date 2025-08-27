package commands.impl.transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.core.Command;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import protocol.ResponseBuilder;
import transaction.TransactionState;
import transaction.TransactionState.QueuedCommand;

/**
 * Implements the Redis EXEC command.
 *
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class ExecCommand extends WriteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecCommand.class);

    private static final String COMMAND_NAME = "EXEC";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argCount(1).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var transactionManager = context.getServerContext().getTransactionManager();
        var clientTransactionState = transactionManager.getOrCreateState(context.getClientChannel());

        if (!clientTransactionState.isInTransaction()) {
            return CommandResult.error(ErrorCode.EXEC_WITHOUT_MULTI.getMessage());
        }

        if (clientTransactionState.isTransactionInvalid()) {
            return handleAbortedTransaction(clientTransactionState);
        }

        var queuedCommands = clientTransactionState.getQueuedCommands();
        clientTransactionState.clearTransaction();
        clientTransactionState.clearWatchedKeys();

        if (queuedCommands.isEmpty()) {
            return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_EMPTY_ARRAY));
        }

        List<ByteBuffer> results = executeQueuedCommands(queuedCommands, context);

        LOGGER.trace("EXEC executed {} queued commands for client={}",
                queuedCommands.size(), context.getClientChannel());

        return CommandResult.success(ResponseBuilder.arrayOfBuffers(results));
    }

    /**
     * Handles case where watched keys were modified and transaction must be
     * aborted.
     */
    private CommandResult handleAbortedTransaction(TransactionState clientTransactionState) {
        clientTransactionState.clearTransaction();
        clientTransactionState.clearWatchedKeys();
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_NULL_ARRAY));
    }

    /**
     * Executes queued commands and collects their responses.
     */
    private List<ByteBuffer> executeQueuedCommands(List<QueuedCommand> queuedCommands, CommandContext context) {
        List<ByteBuffer> results = new ArrayList<>();

        for (var queuedCommand : queuedCommands) {
            CommandResult result = executeQueuedCommand(queuedCommand, context);
            results.add(mapResultToResponse(result));
        }

        return results;
    }

    /**
     * Executes a single queued command.
     */
    private CommandResult executeQueuedCommand(QueuedCommand queuedCommand, CommandContext context) {
        CommandContext commandContext = new CommandContext(
                queuedCommand.operation(),
                queuedCommand.rawArgs(),
                context.getClientChannel(),
                context.getStorageService(),
                context.getServerContext());

        CommandResult result = queuedCommand.command().execute(commandContext);

        if (shouldLogToAof(queuedCommand.command())
                && result instanceof CommandResult.Success
                && context.getServerContext().isAofMode()) {
            var aofRepository = context.getServerContext().getAofRepository();
            if (aofRepository != null) {
                aofRepository.appendCommand(queuedCommand.rawArgs());
            }
        }

        return result;
    }

    /**
     * Converts a CommandResult into RESP-encoded ByteBuffer response.
     */
    private ByteBuffer mapResultToResponse(CommandResult result) {
        return switch (result) {

            case CommandResult.Success success -> success.response();
            case CommandResult.Error error -> ResponseBuilder.error(error.message());
            case CommandResult.MultiSuccess multiSuccess -> ResponseBuilder.merge(multiSuccess.responses());
            case CommandResult.Async _ -> ResponseBuilder.error(ErrorCode.BLOCKING_IN_TRANSACTION.getMessage());
        };
    }

    /**
     * Determines whether a command should be persisted to AOF.
     */
    private boolean shouldLogToAof(Command command) {
        return command.isWriteCommand()
                && !command.isPubSubCommand()
                && !command.isReplicationCommand();
    }
}
