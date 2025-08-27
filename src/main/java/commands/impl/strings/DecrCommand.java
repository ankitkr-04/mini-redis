package commands.impl.strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis INCR command.
 * <p>
 * Increments the integer value of a key by one. If the key does not exist, it
 * is set to 0 before performing the operation.
 * Returns an error if the value is not an integer or cannot be incremented.
 * </p>
 */
public final class DecrCommand extends WriteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncrCommand.class);
    private static final String COMMAND_NAME = "DECR";
    private static final int EXPECTED_ARG_COUNT = 2;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argCount(EXPECTED_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        try {
            long decrementedValue = context.getStorageService().decrementString(key);
            publishDataAdded(key, context.getServerContext());
            propagateCommand(context.getArgs(), context.getServerContext());

            LOGGER.debug("Key '{}' decremented, new value: {}", key, decrementedValue);
            return CommandResult.success(ResponseBuilder.integer(decrementedValue));
        } catch (IllegalStateException | NumberFormatException exception) {
            LOGGER.info("Failed to decrement key '{}': {}", key, exception.getMessage());
            return CommandResult.error(exception.getMessage());
        }
    }
}
