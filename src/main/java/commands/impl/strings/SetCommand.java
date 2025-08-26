package commands.impl.strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;
import storage.expiry.ExpiryPolicy;

/**
 * Implements the Redis SET command.
 * 
 * Sets a string value for a key, with optional PX (expiry in milliseconds).
 * Only the PX modifier is supported for expiry.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class SetCommand extends WriteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetCommand.class);

    private static final String COMMAND_NAME = "SET";
    private static final String EXPIRY_MODIFIER_PX = "PX";
    private static final int MIN_ARGUMENTS = 3;
    private static final int MAX_ARGUMENTS = 5;
    private static final int INDEX_EXPIRY_MODIFIER = 3;
    private static final int INDEX_EXPIRY_VALUE = 4;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    /**
     * Validates the SET command arguments.
     * Supports basic set and set with PX expiry.
     */
    @Override
    protected ValidationResult performValidation(CommandContext context) {

        return CommandValidator.argCount(MIN_ARGUMENTS).or(
                CommandValidator.argCount(MAX_ARGUMENTS)
                        .and(CommandValidator.argEquals(INDEX_EXPIRY_MODIFIER, EXPIRY_MODIFIER_PX))
                        .and(ctx -> CommandValidator.validateInteger(ctx.getArg(INDEX_EXPIRY_VALUE))))
                .validate(context);
    }

    /**
     * Executes the SET command.
     * Stores the value for the given key, with optional PX expiry.
     */
    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        String value = context.getValue();

        ExpiryPolicy expiryPolicy = ExpiryPolicy.never();

        if (context.getArgCount() == MAX_ARGUMENTS
                && EXPIRY_MODIFIER_PX.equalsIgnoreCase(context.getArg(INDEX_EXPIRY_MODIFIER))) {
            long expiryMillis = Long.parseLong(context.getArg(INDEX_EXPIRY_VALUE));
            expiryPolicy = ExpiryPolicy.inMillis(expiryMillis);
            LOGGER.info("Set key '{}' with PX expiry {} ms", key, expiryMillis);
        }

        context.getStorageService().setString(key, value, expiryPolicy);
        publishDataAdded(key, context.getServerContext());
        propagateCommand(context.getArgs(), context.getServerContext());

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}