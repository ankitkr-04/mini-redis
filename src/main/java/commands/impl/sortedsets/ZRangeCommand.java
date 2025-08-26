package commands.impl.sortedsets;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import collections.QuickZSet;
import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis ZRANGE command for sorted sets.
 * <p>
 * Returns a range of members in a sorted set, optionally including their
 * scores.
 * </p>
 * <ul>
 * <li>ZRANGE key start stop [WITHSCORES]</li>
 * <li>Returns members in the specified range, by index, with optional
 * scores.</li>
 * </ul>
 */
public class ZRangeCommand extends ReadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZRangeCommand.class);

    private static final String COMMAND_NAME = "ZRANGE";
    private static final int ARG_INDEX_KEY = 1;
    private static final int ARG_INDEX_START = 2;
    private static final int ARG_INDEX_STOP = 3;
    private static final int ARG_INDEX_WITHSCORES = 4;
    private static final int MIN_ARG_COUNT = 4;
    private static final int MAX_ARG_COUNT = 5;
    private static final String ARG_WITHSCORES = "WITHSCORES";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Validate argument count
        return CommandValidator.argRange(MIN_ARG_COUNT, MAX_ARG_COUNT)
                .and(CommandValidator.intArg(ARG_INDEX_START, ARG_INDEX_STOP)).and(
                        CommandValidator.when(ctx -> ctx.getArgCount() == MAX_ARG_COUNT,
                                CommandValidator.argEquals(ARG_INDEX_WITHSCORES, ARG_WITHSCORES)))
                .validate(context);

    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(ARG_INDEX_KEY);
        int startIndex = context.getIntArg(ARG_INDEX_START);
        int stopIndex = context.getIntArg(ARG_INDEX_STOP);
        boolean includeScores = context.getArgCount() == MAX_ARG_COUNT
                && ARG_WITHSCORES.equalsIgnoreCase(context.getArg(ARG_INDEX_WITHSCORES));

        var storageService = context.getStorageService();
        List<QuickZSet.ZSetEntry> zsetEntries = storageService.zRange(key, startIndex, stopIndex);

        List<String> responseList = new ArrayList<>();
        for (QuickZSet.ZSetEntry zsetEntry : zsetEntries) {
            responseList.add(zsetEntry.member());
            if (includeScores) {
                responseList.add(String.valueOf(zsetEntry.score()));
            }
        }

        LOGGER.debug("ZRANGE executed for key '{}', start {}, stop {}, withScores: {}", key, startIndex, stopIndex,
                includeScores);

        return CommandResult.success(ResponseBuilder.array(responseList));
    }
}
