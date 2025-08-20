package commands.streams;

import java.util.Map;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.expiry.ExpiryPolicy;
import storage.interfaces.StorageEngine;

public class AddStreamCommand implements Command {

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
            return new CommandResult.Success(ResponseWriter.bulkString(entryId));
        } catch (IllegalArgumentException e) {
            // storage throws if semantic rules fail
            return new CommandResult.Error(e.getMessage());
        }
    }

    @Override
    public boolean validate(CommandArgs args) {
        // must have: XADD key id field value [field value ...]
        if (args.argCount() < 4)
            return false;
        if ((args.argCount() - 3) % 2 != 0)
            return false;

        // ID format check (syntax only)
        String id = args.arg(2);
        if (!id.equals("*")) {
            String[] parts = id.split("-");
            if (parts.length != 2)
                return false;

            try {
                Long.parseLong(parts[0]);
                Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }
}
