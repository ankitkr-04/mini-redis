package commands.list;

import java.nio.ByteBuffer;
import commands.ICommand;
import resp.RESPFormatter;
import store.DataStore;

public final class LPopCommand implements ICommand {

    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        var key = args[1];

        var value = dataStore.popFromListLeft(key);
        return RESPFormatter.bulkString(value.orElse(null));
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 2; // "LPOP" + key
    }

}
