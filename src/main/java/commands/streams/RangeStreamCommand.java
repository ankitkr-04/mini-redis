package commands.streams;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import common.ValidationUtil;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class RangeStreamCommand implements Command {

    @Override
    public String name() {
        return "XRANGE";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        int count = 0;

        if (args.argCount() == 6) {
            count = args.getNumericValue(5);
        }

        var res = (count > 0) ? storage.getStreamRange(args.arg(1), args.arg(2), args.arg(3), count)
                : storage.getStreamRange(args.arg(1), args.arg(2), args.arg(3));

        return new CommandResult.Success(
                ResponseWriter.streamEntries(res, e -> e.id(), e -> e.fieldList()));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 4
                || (args.argCount() == 6 && ValidationUtil.isValidInteger(args.arg(5)));
    }
}
