package commands.strings;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class GetCommand implements Command {
    @Override
    public String name() {
        return "GET";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        var value = storage.getString(args.key());
        return new CommandResult.Success(ResponseWriter.bulkString(value.orElse(null)));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 2;
    }
}
