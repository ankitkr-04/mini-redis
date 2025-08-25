package commands.impl.transaction;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import protocol.ResponseBuilder;

public final class WatchCommand extends WriteCommand {
    @Override
    public String getName() {
        return "WATCH";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        if (context.getArgCount() < 2) {
            return ValidationResult.invalid(ErrorCode.WRONG_ARG_COUNT.format("WATCH"));
        }
        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var transactionManager = context.getServerContext().getTransactionManager();
        var clientChannel = context.getClientChannel();

        // WATCH is not allowed inside MULTI
        if (transactionManager.isInTransaction(clientChannel)) {
            return CommandResult.error(ErrorCode.WATCH_INSIDE_MULTI.getMessage());
        }

        // Add all specified keys to the watch list
        for (int i = 1; i < context.getArgCount(); i++) {
            String key = context.getArg(i);
            transactionManager.watchKey(clientChannel, key);
        }

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}
