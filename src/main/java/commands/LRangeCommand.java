package commands;

import java.nio.ByteBuffer;
import java.util.List;
import resp.RESPFormatter;
import store.DataStore;

public final class LRangeCommand implements ICommand {
    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        String key = args[1];
        int start = Integer.parseInt(args[2]);
        int end = Integer.parseInt(args[3]);

        List<String> result = dataStore.getListRange(key, start, end);
        return RESPFormatter.array(result);
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 4; // "LRANGE" + key + start + end
    }
}
