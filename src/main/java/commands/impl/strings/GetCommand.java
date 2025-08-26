package commands.impl.strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Handles the Redis GET command.
 * Retrieves the value of a key as a bulk string response.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class GetCommand extends ReadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetCommand.class);
    private static final String COMMAND_NAME = "GET";
    private static final int MIN_ARGUMENT_COUNT = 2;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argCount(MIN_ARGUMENT_COUNT).validate(context);
    }

    /**
     * Executes the GET command to retrieve the value associated with the given key.
     *
     * @param context the command execution context containing key and storage
     *                service
     * @return the command result with the bulk string response
     */
    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var storageService = context.getStorageService();
        var key = context.getKey();
        var value = storageService.getString(key);

        LOGGER.debug("GET command executed for key: {}", key);

        return CommandResult.success(ResponseBuilder.bulkString(value.orElse(null)));
    }
}