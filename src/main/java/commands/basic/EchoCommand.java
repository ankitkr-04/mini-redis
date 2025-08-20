package commands.basic;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class EchoCommand implements Command {
    @Override
    public String name() {
        return "ECHO";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        return new CommandResult.Success(ResponseWriter.bulkString(args.arg(1)));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 2;
    }
}
