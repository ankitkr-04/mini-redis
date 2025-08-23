package protocol;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import commands.registry.CommandRegistry;
import errors.ErrorCode;
import storage.StorageService;

public final class CommandDispatcher {
    private final CommandRegistry registry;
    private final StorageService storage;

    public CommandDispatcher(CommandRegistry registry, StorageService storage) {
        this.registry = registry;
        this.storage = storage;
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

        if (!command.validate(args)) {
            return ResponseBuilder.error(ErrorCode.WRONG_ARG_COUNT.format(commandName));
        }

        CommandResult result = command.execute(args, storage);

        return switch (result) {
            case CommandResult.Success(var response) -> response;
            case CommandResult.Error(var message) -> ResponseBuilder.error(message);
            case CommandResult.Async() -> null; // No immediate response for blocking commands
        };
    }
}
