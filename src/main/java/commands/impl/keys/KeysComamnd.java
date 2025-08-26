package commands.impl.keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis KEYS command.
 * <p>
 * Returns all keys matching the given pattern from the storage service.
 * </p>
 */
public class KeysComamnd extends ReadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeysComamnd.class);
    private static final String COMMAND_NAME = "KEYS";
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
        String pattern = context.getArg(1);
        var keys = context.getStorageService().getKeysByPattern(pattern);

        LOGGER.debug("Retrieved {} keys matching pattern '{}'", keys.size(), pattern);

        return CommandResult.success(ResponseBuilder.array(keys));
    }
}
