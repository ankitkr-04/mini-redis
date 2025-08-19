package commands;

import java.nio.ByteBuffer;
import responses.StandardResponses;
import store.DataStore;

public final class PingCommand implements ICommand {
    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        return StandardResponses.PONG.duplicate();
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 1;
    }
}
