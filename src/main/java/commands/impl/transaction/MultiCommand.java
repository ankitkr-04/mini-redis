package commands.impl.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import protocol.ResponseBuilder;

/**
 * Handles the MULTI command to start a Redis transaction for a client.
 * If a transaction is already active for the client, returns an error.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class MultiCommand extends WriteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiCommand.class);

    private static final String COMMAND_NAME = "MULTI";
    private static final int MULTI_COMMAND_ARG_COUNT = 1;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argCount(MULTI_COMMAND_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var transactionManager = context.getServerContext().getTransactionManager();
        var clientChannel = context.getClientChannel();
        var clientTransactionState = transactionManager.getOrCreateState(clientChannel);

        if (clientTransactionState.isInTransaction()) {
            LOGGER.info("MULTI command rejected: transaction already active for client {}", clientChannel);
            return CommandResult.error(ErrorCode.NESTED_MULTI.getMessage());
        }

        transactionManager.beginTransaction(clientChannel);
        LOGGER.debug("Transaction started for client {}", clientChannel);
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}
