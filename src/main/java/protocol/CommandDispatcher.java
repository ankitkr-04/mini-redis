package protocol;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import commands.context.CommandContext;
import commands.core.Command;
import commands.impl.transaction.DiscardCommand;
import commands.impl.transaction.ExecCommand;
import commands.impl.transaction.MultiCommand;
import commands.registry.CommandRegistry;
import commands.result.CommandResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import server.ServerContext;
import storage.StorageService;
import transaction.TransactionManager;
import transaction.TransactionState;

public final class CommandDispatcher {
    private final CommandRegistry registry;
    private final StorageService storage;
    private final TransactionManager transactionManager;
    private final ServerContext context;

    public CommandDispatcher(CommandRegistry registry,
            StorageService storage,
            TransactionManager transactionManager,
            ServerContext context) {
        this.registry = registry;
        this.storage = storage;
        this.transactionManager = transactionManager;
        this.context = context;
    }

    public ByteBuffer dispatch(String[] rawArgs, SocketChannel clientChannel) {
        return dispatch(rawArgs, clientChannel, false);
    }

    public ByteBuffer dispatch(String[] rawArgs,
            SocketChannel clientChannel,
            boolean isPropagatedCommand) {
        if (rawArgs == null || rawArgs.length == 0) {
            return ResponseBuilder.error(ErrorCode.UNKNOWN_COMMAND.getMessage());
        }

        String commandName = rawArgs[0].toUpperCase();
        Command command = registry.getCommand(commandName);

        if (command == null) {
            return isPropagatedCommand
                    ? null
                    : ResponseBuilder.error(ErrorCode.UNKNOWN_COMMAND.format(commandName));
        }

        CommandContext cmdContext = new CommandContext(commandName, rawArgs, clientChannel, storage, context);

        if (!command.validate(cmdContext)) {
            return isPropagatedCommand
                    ? null
                    : ResponseBuilder.error(ErrorCode.WRONG_ARG_COUNT.getMessage());
        }

        return executeCommand(command, cmdContext, clientChannel, isPropagatedCommand);
    }

    private ByteBuffer executeCommand(Command command,
            CommandContext context,
            SocketChannel clientChannel,
            boolean isPropagatedCommand) {
        TransactionState transactionState = transactionManager.getOrCreateState(clientChannel);

        if (shouldQueueInTransaction(transactionState, command)) {
            transactionState.queueCommand(command, context);
            return ResponseBuilder.encode(ProtocolConstants.RESP_QUEUED);
        }

        CommandResult result = command.execute(context);
        boolean shouldSendResponse = !isPropagatedCommand || command.isReplicationCommand();

        return shouldSendResponse ? buildResponse(result) : null;
    }

    private boolean shouldQueueInTransaction(TransactionState state, Command command) {
        return state.isInTransaction() && !isTransactionControlCommand(command);
    }

    private boolean isTransactionControlCommand(Command command) {
        return command instanceof MultiCommand ||
                command instanceof ExecCommand ||
                command instanceof DiscardCommand;
    }

    private ByteBuffer buildResponse(CommandResult result) {
        return switch (result) {
            case CommandResult.Success(var response) -> response;
            case CommandResult.Error(var message) -> ResponseBuilder.error(message);
            case CommandResult.Async() -> null; // No immediate response for async commands
        };
    }
}