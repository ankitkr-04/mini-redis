package commands.impl.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis TYPE command.
 * <p>
 * Returns the type of the value stored at the specified key.
 * </p>
 */
public final class TypeCommand extends ReadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeCommand.class);
    private static final String COMMAND_NAME = "TYPE";
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
        var type = context.getStorageService().getType(context.getKey());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("TYPE command executed for key: {}, type: {}", context.getKey(), type.getDisplayName());
        }
        return CommandResult.success(ResponseBuilder.simpleString(type.getDisplayName()));
    }
}
