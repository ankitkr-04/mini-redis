package commands.impl.transaction;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import protocol.ResponseBuilder;

/**
 * Implements the DISCARD command for Redis transactions.
 * 
 * <p>
 * DISCARD flushes all previously queued commands in a transaction and cancels
 * the transaction state.
 * </p>
 * 
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public final class DiscardCommand extends WriteCommand {

    /** Command name constant */
    private static final String COMMAND_NAME = "DISCARD";

    /** Expected argument count for DISCARD command */
    private static final int EXPECTED_ARG_COUNT = 1;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    /**
     * Validates the argument count for the DISCARD command.
     *
     * @param context the command context
     * @return validation result
     */
    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argCount(EXPECTED_ARG_COUNT).validate(context);
    }

    /**
     * Executes the DISCARD command, clearing the transaction state and watched
     * keys.
     *
     * @param context the command context
     * @return command result with success or error response
     */
    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var transactionManager = context.getServerContext().getTransactionManager();
        var transactionState = transactionManager.getOrCreateState(context.getClientChannel());

        if (!transactionState.isInTransaction()) {
            return CommandResult.error(ErrorCode.DISCARD_WITHOUT_MULTI.getMessage());
        }

        transactionState.clearTransaction();
        transactionState.clearWatchedKeys();
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}