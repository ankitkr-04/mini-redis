package command.util;

import java.nio.ByteBuffer;
import command.ICommand;
import store.DataStore;

public final class TypeCommand implements ICommand {

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 2;
    }

    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        
    }

}
