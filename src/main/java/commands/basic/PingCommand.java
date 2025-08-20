package commands.basic;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class PingCommand implements Command {

    @Override
    public String name() {
        return "PING";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        return new CommandResult.Success(ResponseWriter.simpleString("PONG"));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 1;
    }
}
