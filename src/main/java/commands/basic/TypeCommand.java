package commands.basic;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class TypeCommand implements Command {
    @Override
    public String name() {
        return "TYPE";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        var type = storage.getType(args.key());
        return new CommandResult.Success(ResponseWriter.simpleString(type.getDisplayName()));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 2;
    }
}
