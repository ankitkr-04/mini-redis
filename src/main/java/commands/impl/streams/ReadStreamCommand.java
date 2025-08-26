package commands.impl.streams;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.BlockingCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;
import storage.StorageService;

/**
 * Implements the Redis XREAD command for reading entries from one or more
 * streams.
 * <p>
 * Features:
 * <ul>
 * <li>Reads entries added after specified IDs for given streams.</li>
 * <li>Supports blocking reads with optional timeout.</li>
 * <li>Allows limiting the number of entries returned per stream.</li>
 * <li>Returns RESP-encoded responses.</li>
 * </ul>
 * </p>
 *
 * Example use case:
 * 
 * <pre>
 *   XREAD COUNT 10 STREAMS mystream 0
 * </pre>
 * 
 * Reads up to 10 new entries from "mystream" after ID 0.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class ReadStreamCommand extends BlockingCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadStreamCommand.class);

    private static final String COMMAND_NAME = "XREAD";
    private static final int DEFAULT_NO_LIMIT = -1;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        try {
            XReadArgs.parse(context);
            return ValidationResult.valid();
        } catch (IllegalArgumentException e) {
            LOGGER.debug("XREAD validation failed: {}", e.getMessage());
            return ValidationResult.invalid(e.getMessage());
        }
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        XReadArgs inputArgs;
        try {
            inputArgs = XReadArgs.parse(context);
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Failed to parse XREAD arguments: {}", e.getMessage());
            return CommandResult.error(e.getMessage());
        }

        XReadArgs resolvedArgs = inputArgs.withResolvedIds(context.getStorageService());

        List<ByteBuffer> streamResponses = buildStreamResponses(resolvedArgs, context.getStorageService());

        // Non-blocking or immediate return if responses available
        if (!streamResponses.isEmpty() || inputArgs.blockMilliseconds().isEmpty()) {
            return CommandResult.success(
                    streamResponses.isEmpty()
                            ? ResponseBuilder.encode(ProtocolConstants.RESP_EMPTY_ARRAY)
                            : ResponseBuilder.arrayOfBuffers(streamResponses));
        }

        Optional<Long> blockTimeout = inputArgs.blockMilliseconds().filter(ms -> ms > 0);

        context.getServerContext().getBlockingManager().blockClientForStreams(
                resolvedArgs.streamKeys(),
                resolvedArgs.streamIds(),
                inputArgs.count(),
                context.getClientChannel(),
                blockTimeout);

        LOGGER.trace("Client blocked for XREAD: keys={}, ids={}, timeout={}",
                resolvedArgs.streamKeys(), resolvedArgs.streamIds(), blockTimeout.orElse(null));

        return CommandResult.async();
    }

    /**
     * Builds RESP-encoded stream responses for the given XREAD arguments.
     *
     * @param xReadArgs parsed and resolved XREAD arguments
     * @param storage   the storage service used to fetch stream entries
     * @return RESP-encoded responses per stream, one buffer per stream with new
     *         entries
     */
    private List<ByteBuffer> buildStreamResponses(XReadArgs xReadArgs, StorageService storage) {
        List<ByteBuffer> responses = new ArrayList<>();

        for (int index = 0; index < xReadArgs.streamKeys().size(); index++) {
            String streamKey = xReadArgs.streamKeys().get(index);
            String lastSeenId = xReadArgs.streamIds().get(index);

            int fetchLimit = xReadArgs.count().orElse(DEFAULT_NO_LIMIT);

            var newEntries = storage.getStreamAfter(streamKey, lastSeenId, fetchLimit);

            if (!newEntries.isEmpty()) {
                responses.add(
                        ResponseBuilder.arrayOfBuffers(List.of(
                                ResponseBuilder.bulkString(streamKey),
                                ResponseBuilder.streamEntries(newEntries,
                                        entry -> entry.entryId(),
                                        entry -> entry.fields()))));
            }
        }
        return responses;
    }
}
