package command.string;

import java.nio.ByteBuffer;
import command.ICommand;
import datatype.ExpiringMetadata;
import response.StandardResponses;
import store.DataStore;

public final class SetCommand implements ICommand {

    @Override
    public ByteBuffer execute(String[] args, DataStore dataStore) {
        if (!validateArgs(args)) {
            throw new IllegalArgumentException("Invalid SET arguments");
        }

        String key = args[1];
        String value = args[2];

        ExpiringMetadata metadata;
        if (args.length == 3) {
            // No PX -> never expires
            metadata = ExpiringMetadata.never();
        } else {
            // args.length == 5 and PX is guaranteed by validateArgs
            long expiryMs = Long.parseLong(args[4]);
            metadata = ExpiringMetadata.in(expiryMs);
        }

        dataStore.set(key, value, metadata);
        return StandardResponses.OK.duplicate();
    }

    @Override
    public boolean validateArgs(String[] args) {
        return args.length == 3 || (args.length == 5 && "PX".equalsIgnoreCase(args[3]));
    }

}
