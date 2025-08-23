package commands.streams;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import blocking.StreamBlockingManager;
import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import server.protocol.ResponseWriter;
import storage.interfaces.StorageEngine;

public final class ReadStreamCommand implements Command {
    private final StreamBlockingManager blockingManager;

    public ReadStreamCommand(StreamBlockingManager blockingManager) {
        this.blockingManager = blockingManager;
    }

    @Override
    public String name() {
        return "XREAD";
    }

    @Override
    public boolean requiresClient() {
        return true;
    }

    @Override
    public CommandResult execute(CommandArgs args, StorageEngine storage) {
        XReadArgs parsed;
        try {
            parsed = XReadArgs.parse(args);
        } catch (IllegalArgumentException e) {
            return new CommandResult.Error(e.getMessage());
        }

        // Try immediate (non-blocking) read first
        List<ByteBuffer> streamResponses = buildStreamResponses(parsed, storage);

        // If we have data OR no blocking requested, return immediately
        if (!streamResponses.isEmpty() || parsed.blockMs().isEmpty()) {
            return new CommandResult.Success(
                    streamResponses.isEmpty()
                            ? ResponseWriter.arrayOfBuffers() // *0 for no data
                            : ResponseWriter.arrayOfBuffers(streamResponses));
        }

        // No immediate data and blocking requested - block the client
        var timeoutForBlocking = parsed.blockMs().filter(ms -> ms > 0);
        blockingManager.blockClientForStreams(
                parsed.keys(),
                parsed.ids(),
                args.clientChannel(),
                timeoutForBlocking);

        return new CommandResult.Async(); // No immediate response
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
                    : storage.getStreamAfter(key, afterId, -1); // -1 means no limit

            if (!entries.isEmpty()) {
                responses.add(
                        ResponseWriter.arrayOfBuffers(
                                ResponseWriter.bulkString(key),
                                ResponseWriter.streamEntries(entries,
                                        e -> e.id(),
                                        e -> e.fieldList())));
            }
        }
        return responses;
    }

    @Override
    public boolean validate(CommandArgs args) {
        try {
            XReadArgs.parse(args); // This will throw if invalid
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
