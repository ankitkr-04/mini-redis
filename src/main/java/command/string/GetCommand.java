package command.string;

import java.nio.ByteBuffer;
import command.ICommand;
import resp.RESPFormatter;
import response.StandardResponses;
import store.DataStore;

public final class GetCommand implements ICommand {

    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        return dataStore.get(args[1]).map(RESPFormatter::bulkString)
                .orElse(StandardResponses.NULL.duplicate());
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 2; // "GET" + key
    }

}
