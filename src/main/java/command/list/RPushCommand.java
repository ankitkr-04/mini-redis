package command.list;

import java.nio.ByteBuffer;
import java.util.Arrays;
import command.ICommand;
import resp.RESPFormatter;
import store.DataStore;
import util.BlockingClientUtil;

public final class RPushCommand implements ICommand {

    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        String key = args[1];
        String[] values = Arrays.copyOfRange(args, 2, args.length);
        int newSize = dataStore.pushToList(key, values);
        BlockingClientUtil.wakeBlockedClients(key, dataStore);
        return RESPFormatter.integer(newSize);
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length >= 3; // "RPUSH" + key + at least one value
    }

}
