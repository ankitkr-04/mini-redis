package commands.string;

import java.nio.ByteBuffer;
import commands.ICommand;
import records.ExpiringValue;
import responses.StandardResponses;
import store.DataStore;

public final class SetCommand implements ICommand {

    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        if (!validateArgs(args)) {
            throw new IllegalArgumentException("Invalid SET arguments");
        }

        String key = args[1];
        String value = args[2];

        ExpiringValue expiringValue;
        if (args.length == 3) {
            expiringValue = ExpiringValue.withoutExpiry(value);
        } else { // args.length == 5 and PX is guaranteed by validateArgs
            long expiryMs = Long.parseLong(args[4]);
            expiringValue = ExpiringValue.withExpiry(value, expiryMs);
        }

        dataStore.set(key, expiringValue);
        return StandardResponses.OK.duplicate();
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 3 || (args.length == 5 && "PX".equalsIgnoreCase(args[3]));
    }

}
