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

        Map<String, String> fields;
        try {
            fields = args.fieldValueMap(3);
        } catch (IllegalArgumentException e) {
            return new CommandResult.Error("ERR wrong number of arguments for field-value pairs");
        }



        String entryId = storage.addStreamEntry(key, id, fields, ExpiryPolicy.never());

        return new CommandResult.Success(ResponseWriter.bulkString(entryId));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() >= 4;
        // XADD <key> <id|*> field value [field value ...]
    }
}
