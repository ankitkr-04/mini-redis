package commands.lists;

import java.util.List;
import java.util.Optional;
import blocking.ListBlockingManager;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import common.ValidationUtil;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class BlockingPopCommand implements Command {
    private final ListBlockingManager blockingManager;

    public BlockingPopCommand(ListBlockingManager blockingManager) {
        this.blockingManager = blockingManager;
    }

    @Override
    public String name() {
        return "BLPOP";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        String key = args.key();
        double timeoutSeconds = Double.parseDouble(args.arg(2));

        // Try immediate pop
        if (storage.getListLength(key) > 0) {
            var value = storage.leftPop(key);
            if (value.isPresent()) {
                return new CommandResult.Success(ResponseWriter.array(List.of(key, value.get())));
            }
        }

        // Block the client
        double timeoutMs = timeoutSeconds * 1000;

        blockingManager.blockClient(key, args.clientChannel(),
                timeoutMs == 0 ? Optional.empty() : Optional.of(timeoutMs));


        return new CommandResult.Async(); // No immediate response
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 3 && ValidationUtil.isValidTimeout(args.arg(2));
    }

    @Override
    public boolean requiresClient() {
        return true;
    }
}
