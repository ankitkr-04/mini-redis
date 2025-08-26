package commands.impl.lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.WriteCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis LPUSH and RPUSH commands to add one or more values to a
 * list.
 * Determines the direction (left or right) based on the operation name in the
 * context.
 * Returns the new length of the list after the push operation.
 */
public final class PushCommand extends WriteCommand {

    private static final Logger logger = LoggerFactory.getLogger(PushCommand.class);

    private static final String COMMAND_NAME = "PUSH";
    private static final String LEFT_PUSH_OPERATION = "LPUSH";
    private static final int MIN_ARGUMENTS = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.minArgs( MIN_ARGUMENTS).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String key = context.getKey();
        String[] elementsToPush = context.getValues();
        boolean pushToLeft = LEFT_PUSH_OPERATION.equalsIgnoreCase(context.getOperation());

        int listSize = pushToLeft
                ? context.getStorageService().leftPush(key, elementsToPush)
                : context.getStorageService().rightPush(key, elementsToPush);

        publishDataAdded(key, context.getServerContext());
        propagateCommand(context.getArgs(), context.getServerContext());

        logger.debug("Pushed {} element(s) to {} of list '{}', new size: {}",
                elementsToPush.length, pushToLeft ? "left" : "right", key, listSize);

        return CommandResult.success(ResponseBuilder.integer(listSize));
    }
}
