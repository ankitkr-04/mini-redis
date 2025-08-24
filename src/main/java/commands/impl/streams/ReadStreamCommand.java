package commands.impl.streams;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import blocking.BlockingManager;
import commands.CommandArgs;
import commands.CommandResult;
import commands.base.BlockingCommand;
import config.ProtocolConstants;
import errors.ServerError;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;

public final class ReadStreamCommand extends BlockingCommand {
    private final BlockingManager blockingManager;

    public ReadStreamCommand(BlockingManager blockingManager) {
        this.blockingManager = blockingManager;
    }

    @Override
    public String name() {
        return "XREAD";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        try {
            XReadArgs.parse(args);
            return ValidationResult.valid();
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid(ServerError.validation(e.getMessage()));
        }
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        XReadArgs parsed;
        try {
            parsed = XReadArgs.parse(args);
        } catch (IllegalArgumentException e) {
            return new CommandResult.Error(e.getMessage());
        }

        XReadArgs resolvedArgs = parsed.withResolvedIds(storage);

        List<ByteBuffer> streamResponses = buildStreamResponses(resolvedArgs, storage);

        if (!streamResponses.isEmpty() || parsed.blockMs().isEmpty()) {
            return new CommandResult.Success(
                    streamResponses.isEmpty()
                            ? ResponseBuilder.encode(ProtocolConstants.RESP_EMPTY_ARRAY)
                            : ResponseBuilder.arrayOfBuffers(streamResponses));
        }

        var timeoutForBlocking = parsed.blockMs().flatMap(ms -> ms > 0 ? Optional.of(ms) : Optional.empty());

        blockingManager.blockClientForStreams(
                resolvedArgs.keys(),
                resolvedArgs.ids(),
                parsed.count(),
                args.clientChannel(),
                timeoutForBlocking);

        return new CommandResult.Async();
    }

    private List<ByteBuffer> buildStreamResponses(XReadArgs parsed, StorageService storage) {
        List<ByteBuffer> responses = new ArrayList<>();

        for (int i = 0; i < parsed.keys().size(); i++) {
            String key = parsed.keys().get(i);
            String afterId = parsed.ids().get(i);

            var entries = parsed.count().isPresent()
                    ? storage.getStreamAfter(key, afterId, parsed.count().get())
                    : storage.getStreamAfter(key, afterId, -1);

            if (!entries.isEmpty()) {
                responses.add(
                        ResponseBuilder.arrayOfBuffers(List.of(
                                ResponseBuilder.bulkString(key),
                                ResponseBuilder.streamEntries(entries,
                                        e -> e.id(),
                                        e -> e.fieldList()))));
            }
        }
        return responses;
    }
}
