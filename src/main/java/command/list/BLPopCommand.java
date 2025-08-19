package command.list;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import command.ICommand;
import resp.RESPFormatter;
import store.DataStore;

public final class BLPopCommand implements ICommand {
    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore, SocketChannel channel) {

        var key = args[1];
        long timeoutSeconds = Long.parseLong(args[2]);
        if (dataStore.getListLength(key) > 0) {
            // List has element, pop and return immediately
            String value = dataStore.popFromListLeft(key).get();
            return RESPFormatter.array(List.of(key, value));
        } else {
            // List empty, register client for blocking
            long timeoutEndMillis =
                    (timeoutSeconds == 0) ? 0 : System.currentTimeMillis() + timeoutSeconds * 1000;
            dataStore.addBlockedClient(key, channel, timeoutEndMillis);
            return null;
        }

    }


    @Override
    public boolean validateArgs(String[] args) {
        if (args.length == 3) {
            try {
                int count = Integer.parseInt(args[2]);
                return count >= 0; // elementCount must be non-negative
            } catch (NumberFormatException e) {
                return false; // invalid integer
            }
        }
        return false; // wrong number of arguments
    }


    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }



}
