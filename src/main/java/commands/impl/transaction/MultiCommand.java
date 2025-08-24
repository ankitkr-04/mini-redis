package commands.impl.transaction;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import errors.ErrorCode;
import protocol.ResponseBuilder;

public final class MultiCommand extends WriteCommand {
    @Override
    public String getName() {
        return "MULTI";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgCount(context, 1);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var state = context.getServerContext().getTransactionManager().getOrCreateState(context.getClientChannel());

        if (state.isInTransaction()) {
            return CommandResult.error(ErrorCode.NESTED_MULTI.getMessage());
        }

        state.beginTransaction();
        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}