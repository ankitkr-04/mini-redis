package commands.impl.transaction;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import protocol.ResponseBuilder;

public final class DiscardCommand extends WriteCommand {
    @Override
    public String getName() {
        return "DISCARD";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 1);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var state = context.getServerContext().getTransactionManager().getOrCreateState(context.getClientChannel());

        if (!state.isInTransaction()) {
            return CommandResult.error(ErrorCode.DISCARD_WITHOUT_MULTI.getMessage());
        }

        state.clearTransaction();
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}