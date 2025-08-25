package commands.impl.transaction;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

public final class UnwatchCommand extends WriteCommand {
    @Override
    public String getName() {
        return "UNWATCH";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        var transactionManager = context.getServerContext().getTransactionManager();
        var clientChannel = context.getClientChannel();

        // Clear all watched keys for this client
        transactionManager.unwatchAllKeys(clientChannel);

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }
}
