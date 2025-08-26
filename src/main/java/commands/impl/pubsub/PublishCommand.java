package commands.impl.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.PubSubCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Handles the Redis PUBLISH command.
 * <p>
 * Publishes a message to a specified channel and returns the number of
 * subscribers
 * that received the message.
 * </p>
 */
public class PublishCommand extends PubSubCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishCommand.class);
    private static final String COMMAND_NAME = "PUBLISH";
    private static final int REQUIRED_ARG_COUNT = 3;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Validates that the correct number of arguments are provided for the PUBLISH
        // command.
        return CommandValidator.argCount(REQUIRED_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String channelName = context.getArgs()[1];
        String messageContent = context.getArgs()[2];

        var pubSubManager = context.getServerContext().getPubSubManager();

        int subscriberCount = pubSubManager.getAllSubscribers(channelName).size();

        pubSubManager.publish(channelName, messageContent);

        LOGGER.debug("Published message to channel '{}', delivered to {} subscribers.", channelName, subscriberCount);

        // Returns the number of subscribers the message was delivered to, as per Redis
        // protocol.
        return CommandResult.success(ResponseBuilder.integer(subscriberCount));
    }
}
