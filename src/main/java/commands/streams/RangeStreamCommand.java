package commands.streams;

import java.nio.ByteBuffer;
import java.util.List;
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

        // Convert stream entries to ByteBuffer list
        List<ByteBuffer> responseList = res.stream().<ByteBuffer>map(r -> {
            // Create field list as ByteBuffers
            List<ByteBuffer> fields =
                    r.fieldList().stream().map(ResponseWriter::bulkString).toList();

            // Create array of [id, fields_array]
            return ResponseWriter.arrayOfBuffers(ResponseWriter.bulkString(r.id()),
                    ResponseWriter.arrayOfBuffers(fields));
        }).toList();

        return new CommandResult.Success(ResponseWriter.arrayOfBuffers(responseList));
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() == 4
                || (args.argCount() == 6 && ValidationUtil.isValidInteger(args.arg(5)));
    }
}
