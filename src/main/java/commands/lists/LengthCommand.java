package commands.lists;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class LengthCommand implements Command {
    @Override
    public String name() { return "LLEN"; }
    
    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        int length = storage.getListLength(args.key());
        return new CommandResult.Success(ResponseWriter.integer(length));
    }
    
    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 2;
    }
}