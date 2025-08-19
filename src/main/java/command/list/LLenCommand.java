package command.list;

import java.nio.ByteBuffer;
import command.ICommand;
import resp.RESPFormatter;
import store.DataStore;

public final class LLenCommand implements ICommand {

    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        String key = args[1];
        int size = dataStore.getListLength(key);

        return RESPFormatter.integer(size);
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 2; // "LLEN" + key
    }

}
