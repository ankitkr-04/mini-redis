package commands.impl.sortedsets;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

public class ZRankCommand extends ReadCommand {
    @Override
    public String getName() {
        return "ZRANK";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // ZRANK key member
        return CommandValidator.validateArgCount(context, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(1);
        String member = context.getArg(2);

        var storage = context.getServerContext().getStorageService();
        Long rank = storage.zRank(key, member);

        if (rank == null) {
            return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_NULL_BULK_STRING));
        }

        return CommandResult.success(ResponseBuilder.integer(rank));
    }

}
