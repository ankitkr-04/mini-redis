package command.list;

import java.nio.ByteBuffer;
import command.ICommand;
import resp.RESPFormatter;
import store.DataStore;

public final class LPopCommand implements ICommand {

    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        var key = args[1];

        if (args.length == 2) {

            var value = dataStore.popFromListLeft(key);
            return RESPFormatter.bulkString(value.orElse(null));
        }
        int elementCount = Integer.parseInt(args[2]);

        var values = dataStore.popFromListLeft(key, elementCount);

        return RESPFormatter.array(values);

    }

    @Override
    public boolean validateArgs(String[] args) {
        if (args.length == 2) {
            return true; // "LPOP key"
        } else if (args.length == 3) {
            try {
                int count = Integer.parseInt(args[2]);
                return count >= 0; // elementCount must be non-negative
            } catch (NumberFormatException e) {
                return false; // invalid integer
            }
        }
        return false; // wrong number of arguments
    }

}
