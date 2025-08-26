package commands.impl.streams;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.context.CommandContext;
import storage.StorageService;

/**
 * Represents parsed arguments for the XREAD command in Redis streams.
 * Supports COUNT, BLOCK, and STREAMS options.
 * Provides parsing and ID resolution utilities.
 */
public record XReadArgs(
    Optional<Integer> count,
    Optional<Long> blockMilliseconds,
    List<String> streamKeys,
    List<String> streamIds) {

    private static final Logger LOGGER = LoggerFactory.getLogger(XReadArgs.class);

    private static final String ARG_COUNT = "COUNT";
    private static final String ARG_BLOCK = "BLOCK";
    private static final String ARG_STREAMS = "STREAMS";
    private static final String STREAM_ID_LATEST = "$";
    private static final String STREAM_ID_ZERO = "0-0";

    /**
     * Parses XREAD command arguments from the given context.
     *
     * @param context CommandContext containing the arguments.
     * @return XReadArgs instance with parsed values.
     * @throws IllegalArgumentException if arguments are invalid.
     */
    public static XReadArgs parse(CommandContext context) {
    int countValue = -1;
    long blockValue = -1;
    int currentIndex = 1;

    while (currentIndex < context.getArgCount() && !context.getArg(currentIndex).equalsIgnoreCase(ARG_STREAMS)) {
        String argument = context.getArg(currentIndex).toUpperCase();
        switch (argument) {
        case ARG_COUNT -> {
            if (currentIndex + 1 >= context.getArgCount()) {
            throw new IllegalArgumentException("COUNT requires a value");
            }
            countValue = Integer.parseInt(context.getArg(currentIndex + 1));
            currentIndex += 2;
        }
        case ARG_BLOCK -> {
            if (currentIndex + 1 >= context.getArgCount()) {
            throw new IllegalArgumentException("BLOCK requires a value");
            }
            blockValue = Long.parseLong(context.getArg(currentIndex + 1));
            currentIndex += 2;
        }
        default -> throw new IllegalArgumentException(
            String.format("Unexpected token: %s", context.getArg(currentIndex)));
        }
    }

    if (currentIndex >= context.getArgCount() || !context.getArg(currentIndex).equalsIgnoreCase(ARG_STREAMS)) {
        throw new IllegalArgumentException("Missing STREAMS keyword");
    }
    currentIndex++;

    int remainingArguments = context.getArgCount() - currentIndex;
    if (remainingArguments % 2 != 0) {
        throw new IllegalArgumentException("Wrong number of arguments for XREAD STREAMS");
    }

    int numberOfKeys = remainingArguments / 2;
    List<String> keys = context.getSlice(currentIndex, currentIndex + numberOfKeys);
    List<String> ids = context.getSlice(currentIndex + numberOfKeys, context.getArgCount());

    LOGGER.debug("Parsed XREAD args: count={}, blockMs={}, keys={}, ids={}",
        countValue, blockValue, keys, ids);

    return new XReadArgs(
        countValue > 0 ? Optional.of(countValue) : Optional.empty(),
        blockValue >= 0 ? Optional.of(blockValue) : Optional.empty(),
        keys,
        ids);
    }

    /**
     * Resolves '$' IDs to the latest stream ID from storage.
     *
     * @param storage StorageService to fetch latest stream IDs.
     * @return XReadArgs with resolved IDs.
     */
    public XReadArgs withResolvedIds(StorageService storage) {
    List<String> resolvedIds = new ArrayList<>(streamIds.size());
    for (int i = 0; i < streamKeys.size(); i++) {
        String key = streamKeys.get(i);
        String id = streamIds.get(i);
        if (STREAM_ID_LATEST.equals(id)) {
        Optional<String> lastId = storage.getLastStreamId(key);
        resolvedIds.add(lastId.orElse(STREAM_ID_ZERO));
        } else {
        resolvedIds.add(id);
        }
    }
    LOGGER.trace("Resolved stream IDs: {}", resolvedIds);
    return new XReadArgs(count, blockMilliseconds, streamKeys, List.copyOf(resolvedIds));
    }
}
