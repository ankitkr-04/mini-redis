package commands.impl.sortedsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the ZADD command for sorted sets.
 * <p>
 * Adds one or more members to a sorted set, or updates their scores if they
 * already exist.
 * The command expects arguments in the form: ZADD key score member [score
 * member ...]
 * </p>
 */
public class ZAddCommand extends WriteCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZAddCommand.class);

    private static final String COMMAND_NAME = "ZADD";
    private static final int MIN_ARGUMENTS = 4; // ZADD key score member
    private static final int KEY_INDEX = 1;
    private static final int FIRST_SCORE_INDEX = 2;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {

        CommandValidator.minArgs(MIN_ARGUMENTS).and(CommandValidator.evenArgCount()).validate(context);

        for (int i = FIRST_SCORE_INDEX; i < context.getArgCount(); i += 2) {
            ValidationResult scoreValidation = CommandValidator.validateDouble(context.getArg(i));
            if (!scoreValidation.isValid()) {
                return scoreValidation;
            }
        }

        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getArg(KEY_INDEX);
        var storageService = context.getStorageService();

        int membersAdded = 0;

        for (int i = FIRST_SCORE_INDEX; i < context.getArgCount(); i += 2) {
            double score = context.getDoubleArg(i);
            String member = context.getArg(i + 1);

            if (storageService.zAdd(key, member, score)) {
                membersAdded++;
            }
        }

        LOGGER.debug("ZADD executed for key '{}', members added: {}", key, membersAdded);

        return CommandResult.success(ResponseBuilder.integer(membersAdded));
    }
}
