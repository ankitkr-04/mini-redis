package commands.impl.sortedsets;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

/**
 * Handles the ZRANK command for sorted sets.
 * <p>
 * Returns the rank (index) of a member in a sorted set, with scores ordered
 * from low to high.
 * </p>
 */
public class ZRankCommand extends ReadCommand {

    private static final String COMMAND_NAME = "ZRANK";
    private static final int EXPECTED_ARG_COUNT = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Validates that the correct number of arguments are provided for ZRANK.
        return CommandValidator.argCount(EXPECTED_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(1);
        String member = context.getArg(2);

        var storageService = context.getServerContext().getStorageService();
        Long memberRank = storageService.zRank(key, member);

        if (memberRank == null) {
            return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_NULL_BULK_STRING));
        }

        return CommandResult.success(ResponseBuilder.integer(memberRank));
    }
}
