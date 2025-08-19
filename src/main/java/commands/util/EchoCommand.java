package commands.util;

import java.nio.ByteBuffer;
import commands.ICommand;
import resp.RESPFormatter;
import store.DataStore;

public final class EchoCommand implements ICommand {
    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        return RESPFormatter.bulkString(args[1]);
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 2; // "ECHO" + message
    }
}
