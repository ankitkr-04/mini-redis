package commands.impl.sortedsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the ZCARD command for sorted sets.
 * <p>
 * Returns the cardinality (number of elements) of the sorted set stored at the
 * specified key.
 * </p>
 */
public class ZCardCommand extends ReadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZCardCommand.class);
    private static final String COMMAND_NAME = "ZCARD";
    private static final int EXPECTED_ARG_COUNT = 2;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Validates that the correct number of arguments are provided for ZCARD
        return CommandValidator.argCount(EXPECTED_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(1);
        var storageService = context.getServerContext().getStorageService();

        int cardinality = storageService.zSize(key);

        LOGGER.debug("ZCARD executed for key '{}', cardinality: {}", key, cardinality);

        return CommandResult.success(ResponseBuilder.integer(cardinality));
    }
}
