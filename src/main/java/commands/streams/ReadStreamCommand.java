package commands.streams;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class ReadStreamCommand implements Command {

    @Override
    public String name() {
        return "XREAD";
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        XReadArgs parsed;
        try {
            parsed = XReadArgs.parse(args);
        } catch (IllegalArgumentException e) {
            return new CommandResult.Error(e.getMessage());
        }

        List<ByteBuffer> streamResponses = buildStreamResponses(parsed, storage);

        if (streamResponses.isEmpty()) {
            return new CommandResult.Success(ResponseWriter.arrayOfBuffers()); // *0
        }
        return new CommandResult.Success(ResponseWriter.arrayOfBuffers(streamResponses));
    }

    /**
     * Builds RESP-ready stream responses for given parsed XREAD args.
     */
    private List<ByteBuffer> buildStreamResponses(XReadArgs parsed, StorageEngine storage) {
        List<ByteBuffer> responses = new ArrayList<>();

        for (int i = 0; i < parsed.keys().size(); i++) {
            String key = parsed.keys().get(i);
            String afterId = parsed.ids().get(i);

            var entries = parsed.count().isPresent()
                    ? storage.getStreamAfter(key, afterId, parsed.count().get())
                    : storage.getStreamAfter(key, afterId);

            if (!entries.isEmpty()) {
                responses.add(
                        ResponseWriter.arrayOfBuffers(
                                ResponseWriter.bulkString(key),
                                ResponseWriter.streamEntries(entries, e -> e.id(),
                                        e -> e.fieldList())));
            }
        }
        return responses;
    }

    @Override
    public boolean validate(CommandArgs args) {
        return args.argCount() >= 4;
    }
}
