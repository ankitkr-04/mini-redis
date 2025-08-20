package server.protocol;

import java.nio.ByteBuffer;
import commands.Command;
import commands.CommandArgs;
import commands.CommandRegistry;
import commands.CommandResult;
import common.ErrorMessage;
import storage.interfaces.StorageEngine;

public final class CommandDispatcher {
    private final CommandRegistry registry;
    private final StorageEngine storage;

    public CommandDispatcher(CommandRegistry registry, StorageEngine storage) {
        this.registry = registry;
        this.storage = storage;
    }

    public ByteBuffer dispatch(String[] rawArgs, java.nio.channels.SocketChannel clientChannel) {
        if (rawArgs == null || rawArgs.length == 0) {
            return ResponseWriter.error(ErrorMessage.Command.UNKNOWN_COMMAND);
        }

        String commandName = rawArgs[0].toUpperCase();
        Command command = registry.getCommand(commandName);

        if (command == null) {
            return ResponseWriter.error(
                    String.format(ErrorMessage.Command.UNKNOWN_COMMAND_WITH_NAME, commandName));
        }

        CommandArgs args = new CommandArgs(commandName, rawArgs, clientChannel);

        if (!command.validate(args)) {
            return ResponseWriter
                    .error(String.format(ErrorMessage.Command.WRONG_ARG_COUNT, commandName));
        }

        CommandResult result = command.execute(args, storage);

        return switch (result) {
            case CommandResult.Success(var response) -> response;
            case CommandResult.Error(var message) -> ResponseWriter.error(message);
            case CommandResult.Async() -> null; // No immediate response for blocking commands
        };
    }
}
