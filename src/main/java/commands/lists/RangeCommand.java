package commands.lists;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import common.ValidationUtil;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class RangeCommand implements Command {
    @Override
    public String name() {
        return "LRANGE";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        String key = args.key();
        int start = Integer.parseInt(args.arg(2));
        int end = Integer.parseInt(args.arg(3));

        var values = storage.getListRange(key, start, end);
        return new CommandResult.Success(ResponseWriter.array(values));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 4 && ValidationUtil.isValidInteger(args.arg(2))
                && ValidationUtil.isValidInteger(args.arg(3));
    }
}
