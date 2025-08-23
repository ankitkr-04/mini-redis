package commands.streams;

import java.util.Map;
import blocking.StreamBlockingManager;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.expiry.ExpiryPolicy;
import storage.interfaces.StorageEngine;

public class AddStreamCommand implements Command {

    private final StreamBlockingManager manager;

    public AddStreamCommand(StreamBlockingManager blockingManager) {
        this.manager = blockingManager;
    }

    @Override
    public boolean requiresClient() {
        return true;
    }

    @Override
    public String name() {
        return "XADD";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        String key = args.arg(1);
        String id = args.arg(2);

        Map<String, String> fields = args.fieldValueMap(3);


        try {
            // semantic validation (ID ordering, duplicates) happens inside storage
            String entryId = storage.addStreamEntry(key, id, fields, ExpiryPolicy.never());
            manager.notifyWaitingClients(key);

            return new CommandResult.Success(ResponseWriter.bulkString(entryId));
        } catch (IllegalArgumentException e) {
            // storage throws if semantic rules fail
            return new CommandResult.Error(e.getMessage());
        }
    }

    @Override
    public boolean validate(CommandArgs args) {
        // Must have: XADD key id field value [field value ...]
        if (args.argCount() < 4)
            return false;
        if ((args.argCount() - 3) % 2 != 0)
            return false;

        // Basic ID syntax check
        String id = args.arg(2);
        if (id.equals("*") || id.endsWith("-*") || id.matches("\\d+-\\d+")) {
            return true;
        }
        return false;
    }
}
