package commands.impl.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import protocol.ResponseBuilder;

/**
 * Implements the Redis WATCH command.
 * <p>
 * Adds the specified keys to the client's watch list for transaction support.
 * If the client is already in a MULTI transaction, the command returns an
 * error.
 * </p>
 */
public final class WatchCommand extends WriteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(WatchCommand.class);

    private static final String COMMAND_NAME = "WATCH";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        if (context.getArgCount() < 2) {
            return ValidationResult.invalid(ErrorCode.WRONG_ARG_COUNT.format(COMMAND_NAME));
        }
        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var transactionManager = context.getServerContext().getTransactionManager();
        var clientChannel = context.getClientChannel();

        if (transactionManager.isInTransaction(clientChannel)) {
            LOGGER.debug("WATCH command issued inside MULTI transaction for client: {}", clientChannel);
            return CommandResult.error(ErrorCode.WATCH_INSIDE_MULTI.getMessage());
        }

        int argCount = context.getArgCount();
        for (int argIndex = 1; argIndex < argCount; argIndex++) {
            String keyToWatch = context.getArg(argIndex);
            transactionManager.watchKey(clientChannel, keyToWatch);
            LOGGER.trace("Client {} is now watching key: {}", clientChannel, keyToWatch);
        }

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}
