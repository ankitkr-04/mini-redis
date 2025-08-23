package protocol;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import commands.impl.transaction.DiscardCommand;
import commands.impl.transaction.ExecCommand;
import commands.impl.transaction.MultiCommand;
import commands.registry.CommandRegistry;
import config.RedisConstants;
import errors.ErrorCode;
import storage.StorageService;
import transaction.TransactionManager;
import transaction.TransactionState;

public final class CommandDispatcher {
    private final CommandRegistry registry;
    private final StorageService storage;
    private final TransactionManager transactionManager;

    public CommandDispatcher(CommandRegistry registry, StorageService storage,
            TransactionManager transactionManager) {
        this.registry = registry;
        this.storage = storage;
        this.transactionManager = transactionManager;
    }

    public ByteBuffer dispatch(String[] rawArgs, SocketChannel clientChannel) {
        if (rawArgs == null || rawArgs.length == 0) {
            return ResponseBuilder.error(ErrorCode.UNKNOWN_COMMAND.getMessage());
        }

        String commandName = rawArgs[0].toUpperCase();
        Command command = registry.getCommand(commandName);
        if (command == null) {
            return ResponseBuilder.error(ErrorCode.UNKNOWN_COMMAND.format(commandName));
        }

        CommandArgs args = new CommandArgs(commandName, rawArgs, clientChannel);
        var state = transactionManager.getOrCreateState(clientChannel);

        if (!command.validate(args)) {
            return ResponseBuilder.error(ErrorCode.WRONG_ARG_COUNT.getMessage());
        }

        if (isTransactionalButNotControlCommand(state, command)) {
            return queueTransactionCommand(state, command, args);
        }

        return executeCommand(command, args);
    }

    private boolean isTransactionalButNotControlCommand(
            TransactionState state, Command command) {
        return state.isInTransaction() &&
                !(command instanceof MultiCommand
                        || command instanceof ExecCommand
                        || command instanceof DiscardCommand);
    }

    private ByteBuffer queueTransactionCommand(
            TransactionState state, Command command, CommandArgs args) {
        state.queueCommand(command, args);
        return ResponseBuilder.encode(RedisConstants.QUEUED_RESPONSE);
    }

    private ByteBuffer executeCommand(Command command, CommandArgs args) {
        CommandResult result = command.execute(args, storage);
        return switch (result) {
            case CommandResult.Success(var response) -> response;
            case CommandResult.Error(var message) -> ResponseBuilder.error(message);
            case CommandResult.Async() -> null; // No immediate response for blocking commands
        };
    }
}
