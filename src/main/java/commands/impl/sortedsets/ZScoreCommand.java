package commands.impl.sortedsets;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

/**
 * Handles the ZSCORE command for sorted sets.
 * <p>
 * Returns the score of a member in a sorted set, or null if the member does not
 * exist.
 * </p>
 */
public class ZScoreCommand extends ReadCommand {

    private static final String COMMAND_NAME = "ZSCORE";
    private static final int EXPECTED_ARG_COUNT = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Validates that the correct number of arguments are provided for ZSCORE.
        return CommandValidator.argCount(EXPECTED_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(1);
        String memberName = context.getArg(2);

        var storageService = context.getServerContext().getStorageService();
        Double memberScore = storageService.zScore(key, memberName);

        if (memberScore == null) {
            return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_NULL_BULK_STRING));
        }

        return CommandResult.success(ResponseBuilder.bulkString(String.valueOf(memberScore)));
    }
}
