package commands.impl.pubsub;

import commands.base.PubSubCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class PublishCommand extends PubSubCommand {

    @Override
    public String getName() {
        return "PUBLISH";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // PUBLISH <channel> <message>
        return CommandValidator.validateArgCount(context, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String channel = context.getArgs()[1];
        String message = context.getArgs()[2];

        var pubSubManager = context.getServerContext().getPubSubManager();

        // Gather subscribers *before* publish (to count them accurately)
        int subscribers = pubSubManager.getAllSubscribers(channel).size();

        // Deliver the message
        pubSubManager.publish(channel, message);

        // Redis spec: integer reply = number of clients message was delivered to
        return CommandResult.success(ResponseBuilder.integer(subscribers));
    }
}
