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
        String key = args[1];
        double timeoutSeconds = Double.parseDouble(args[2]);

        if (dataStore.getListLength(key) > 0) {
            String value = dataStore.popFromListLeft(key).get();
            return RESPFormatter.array(List.of(key, value));
        } else {
            double timeoutMs = timeoutSeconds * 1000;
            if (timeoutMs == 0) {
                dataStore.addBlockedClient(key, channel);
            } else {
                dataStore.addBlockedClient(key, channel, timeoutMs);
            }
            return null;
        }
    }

    @Override
    public boolean validateArgs(String[] args) {
        if (args.length == 3) {
            try {
                double timeout = Double.parseDouble(args[2]);
                return timeout >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }


    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }



}
