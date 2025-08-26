package commands.impl.lists;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;
import storage.types.PopDirection;

/**
 * Implements the POP command for Redis-like lists, supporting both single and
 * multiple element pops
 * from either the left (LPOP) or right (RPOP) of the list.
 * 
 * Usage:
 * - LPOP/RPOP key
 * - LPOP/RPOP key count
 */
public final class PopCommand extends WriteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopCommand.class);

    private static final String COMMAND_NAME = "POP";
    private static final int MIN_ARG_COUNT = 2;
    private static final int MAX_ARG_COUNT = 3;
    private static final int COUNT_ARG_INDEX = 2;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argRange(MIN_ARG_COUNT, MAX_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        PopDirection popDirection = resolvePopDirection(context.getOperation());

        if (context.getArgCount() == MIN_ARG_COUNT) {
            Optional<String> poppedValue = popSingleElement(context, key, popDirection);
            LOGGER.debug("Popped single element from key '{}', direction {}: {}", key, popDirection,
                    poppedValue.orElse("null"));
            return CommandResult.success(ResponseBuilder.bulkString(poppedValue.orElse(null)));
        } else {
            int popCount = Integer.parseInt(context.getArg(COUNT_ARG_INDEX));
            List<String> poppedValues = popMultipleElements(context, key, popDirection, popCount);
            LOGGER.debug("Popped {} elements from key '{}', direction {}: {}", popCount, key, popDirection,
                    poppedValues);
            return CommandResult.success(ResponseBuilder.array(poppedValues));
        }
    }

    /**
     * Determines the pop direction (left or right) based on the operation name.
     *
     * @param operation The operation name (e.g., "LPOP" or "RPOP").
     * @return The corresponding PopDirection.
     */
    private PopDirection resolvePopDirection(String operation) {
        return "LPOP".equalsIgnoreCase(operation) ? PopDirection.LEFT : PopDirection.RIGHT;
    }

    /**
     * Pops a single element from the specified key in the given direction.
     *
     * @param context      The command context.
     * @param key          The list key.
     * @param popDirection The direction to pop from.
     * @return An Optional containing the popped value, or empty if the list is
     *         empty.
     */
    private Optional<String> popSingleElement(CommandContext context, String key, PopDirection popDirection) {
        return popDirection.isLeft()
                ? context.getStorageService().leftPop(key)
                : context.getStorageService().rightPop(key);
    }

    /**
     * Pops multiple elements from the specified key in the given direction.
     *
     * @param context      The command context.
     * @param key          The list key.
     * @param popDirection The direction to pop from.
     * @param count        The number of elements to pop.
     * @return A list of popped values.
     */
    private List<String> popMultipleElements(CommandContext context, String key, PopDirection popDirection, int count) {
        return popDirection.isLeft()
                ? context.getStorageService().leftPop(key, count)
                : context.getStorageService().rightPop(key, count);
    }
}