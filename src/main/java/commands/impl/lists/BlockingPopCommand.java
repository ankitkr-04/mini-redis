package commands.impl.lists;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blocking.BlockingManager;
import commands.base.BlockingCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the BLPOP command for blocking list pop in a Redis-like protocol.
 * Attempts to pop an element from the left of the first non-empty list among
 * the given keys.
 * If all lists are empty, blocks the client until an element is available or
 * the timeout expires.
 */
public final class BlockingPopCommand extends BlockingCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingPopCommand.class);

    private static final String COMMAND_NAME = "BLPOP";
    private static final int MIN_ARG_COUNT = 3;

    private final BlockingManager blockingManager;

    public BlockingPopCommand(BlockingManager blockingManager) {
        this.blockingManager = blockingManager;
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        final int TIMER_INDEX = context.getArgCount() - 1;

        return CommandValidator.argRange(MIN_ARG_COUNT, Integer.MAX_VALUE)
                .and(CommandValidator.timeoutArg(TIMER_INDEX)).validate(context);

    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        List<String> keys = context.getSlice(1, context.getArgCount() - 1);
        String timeoutStr = context.getArg(context.getArgCount() - 1);
        double timeoutSec = Double.parseDouble(timeoutStr);
        long timeoutMs = (long) (timeoutSec * 1000);
        Optional<Long> optTimeout = timeoutSec == 0 ? Optional.empty() : Optional.of(timeoutMs);

        for (String key : keys) {
            if (context.getStorageService().getListLength(key) > 0) {
                String value = context.getStorageService().leftPop(key).orElse(null);
                LOGGER.debug("Popped value from key '{}': {}", key, value);
                return CommandResult.success(ResponseBuilder.array(List.of(key, value)));
            }
        }

        LOGGER.info("Blocking client on keys {} with timeout {} ms", keys, optTimeout.orElse(0L));
        blockingManager.blockClientForLists(keys, context.getClientChannel(), optTimeout);
        return CommandResult.async();
    }
}
