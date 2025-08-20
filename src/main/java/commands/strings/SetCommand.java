package commands.strings;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import common.ValidationUtil;
import server.protocol.ResponseWriter;
import storage.expiry.ExpiryPolicy;
import storage.interfaces.StorageEngine;

public final class SetCommand implements Command {
    @Override
    public String name() {
        return "SET";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        String key = args.key();
        String value = args.value();

        ExpiryPolicy expiry;
        if (args.argCount() == 3) {
            expiry = ExpiryPolicy.never();
        } else if (args.argCount() == 5 && "PX".equalsIgnoreCase(args.arg(3))) {
            expiry = ExpiryPolicy.inMillis(Long.parseLong(args.arg(4)));
        } else {
            expiry = ExpiryPolicy.never();
        }
        switch (args.argCount()) {
            case 3:
                expiry = ExpiryPolicy.never();
                break;
            case 5:
                if ("PX".equalsIgnoreCase(args.arg(3))) {
                    expiry = ExpiryPolicy.inMillis(Long.parseLong(args.arg(4)));
                } else {
                    expiry = ExpiryPolicy.never();
                }
                break;
            default:
                expiry = ExpiryPolicy.never();
                break;
        }
        storage.setString(key, value, expiry);
        return new CommandResult.Success(ResponseWriter.simpleString("OK"));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 3 || (args.argCount() == 5 && "PX".equalsIgnoreCase(args.arg(3))
                && ValidationUtil.isValidInteger(args.arg(4)));
    }
}
