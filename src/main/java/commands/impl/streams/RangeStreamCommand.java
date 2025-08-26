package commands.impl.streams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Handles the XRANGE command for retrieving a range of entries from a Redis
 * stream.
 * Supports optional COUNT argument to limit the number of returned entries.
 */
public final class RangeStreamCommand extends ReadCommand {

    private static final Logger logger = LoggerFactory.getLogger(RangeStreamCommand.class);

    private static final String COMMAND_NAME = "XRANGE";
    private static final String COUNT_ARG = "COUNT";
    private static final int MIN_ARG_COUNT = 4;
    private static final int MAX_ARG_COUNT = 6;
    private static final int COUNT_ARG_INDEX = 4;
    private static final int COUNT_VALUE_INDEX = 5;
    private static final int START_INDEX = 2;
    private static final int END_INDEX = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {

        // Case 1: XRANGE <key> <start> <end>
        var basicForm = CommandValidator.argCount(MIN_ARG_COUNT);

        // Case 2: XRANGE <key> <start> <end> COUNT <count>
        var withCount = CommandValidator.argCount(MAX_ARG_COUNT)
                .and(CommandValidator.argEquals(COUNT_ARG_INDEX, COUNT_ARG))
                .and(ctx -> CommandValidator.validateInteger(ctx.getArg(COUNT_VALUE_INDEX)));

        // Combine with OR: either basic form OR with COUNT is valid
        return basicForm.or(withCount).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        int count = context.getArgCount() == MIN_ARG_COUNT ? 0 : context.getIntArg(COUNT_VALUE_INDEX);
        var streamEntries = context.getStorageService().getStreamRange(
                context.getKey(),
                context.getArg(START_INDEX),
                context.getArg(END_INDEX),
                count);
        logger.debug("XRANGE executed for key: {}, start: {}, end: {}, count: {}",
                context.getKey(), context.getArg(START_INDEX), context.getArg(END_INDEX), count);
        return CommandResult.success(
                ResponseBuilder.streamEntries(streamEntries, entry -> entry.entryId(), entry -> entry.fields()));
    }
}