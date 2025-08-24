package commands.impl.streams;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import commands.base.BlockingCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;
import storage.StorageService;

public final class ReadStreamCommand extends BlockingCommand {
    @Override
    public String getName() {
        return "XREAD";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        try {
            XReadArgs.parse(context);
            return ValidationResult.valid();
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid(e.getMessage());
        }
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        XReadArgs parsed;
        try {
            parsed = XReadArgs.parse(context);
        } catch (IllegalArgumentException e) {
            return CommandResult.error(e.getMessage());
        }

        XReadArgs resolvedArgs = parsed.withResolvedIds(context.getStorageService());

        List<ByteBuffer> streamResponses = buildStreamResponses(resolvedArgs, context.getStorageService());

        if (!streamResponses.isEmpty() || parsed.blockMs().isEmpty()) {
            return CommandResult.success(
                    streamResponses.isEmpty()
                            ? ResponseBuilder.encode(ProtocolConstants.RESP_EMPTY_ARRAY)
                            : ResponseBuilder.arrayOfBuffers(streamResponses));
        }

        var timeoutForBlocking = parsed.blockMs().flatMap(ms -> ms > 0 ? Optional.of(ms) : Optional.empty());

        context.getServerContext().getBlockingManager().blockClientForStreams(
                resolvedArgs.keys(),
                resolvedArgs.ids(),
                parsed.count(),
                context.getClientChannel(),
                timeoutForBlocking);

        return CommandResult.async();
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