package commands.lists;

import blocking.BlockingManager;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import common.ErrorMessage;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class PushCommand implements Command {
    private final BlockingManager blockingManager;

    public PushCommand(BlockingManager blockingManager) {
        this.blockingManager = blockingManager;
    }

    @Override
    public String name() {
        return "PUSH"; // Handles both LPUSH and RPUSH
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        String operation = args.operation().toUpperCase();
        String key = args.key();
        String[] values = args.values();

        int newSize = switch (operation) {
            case "LPUSH" -> storage.leftPush(key, values);
            case "RPUSH" -> storage.rightPush(key, values);
            default -> throw new IllegalArgumentException(
                    String.format(ErrorMessage.Command.UNSUPPORTED_OPERATION, operation));
        };

        // Wake up blocked clients
        blockingManager.notifyWaitingClients(key, storage);

        return new CommandResult.Success(ResponseWriter.integer(newSize));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() >= 3;
    }
}
