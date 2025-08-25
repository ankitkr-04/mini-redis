package commands.impl.sortedsets;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

public class ZScoreCommand extends ReadCommand {
    @Override
    public String getName() {
        return "ZSCORE";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // ZSCORE key member

        return CommandValidator.validateArgCount(context, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(1);
        String member = context.getArg(2);

        var storage = context.getServerContext().getStorageService();
        Double score = storage.zScore(key, member);

        if (score == null) {
            return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_NULL_BULK_STRING));
        }

        return CommandResult.success(ResponseBuilder.bulkString(String.valueOf(score)));
    }

}
