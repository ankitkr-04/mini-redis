package commands.impl.pubsub;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.PubSubCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Handles the Redis SUBSCRIBE and PSUBSCRIBE commands.
 * Subscribes the client to one or more channels or patterns and sends an
 * acknowledgment for each.
 */
public class SubscribeCommand extends PubSubCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeCommand.class);

    private static final String COMMAND_NAME = "SUBSCRIBE";
    private static final String PATTERN_COMMAND_NAME = "PSUBSCRIBE";
    private static final String SUBSCRIBE_RESPONSE_TYPE = "subscribe";
    private static final String PSUBSCRIBE_RESPONSE_TYPE = "psubscribe";
    private static final int MIN_ARGUMENTS = 2;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Requires at least command name and one channel/pattern
        return CommandValidator.minArgs(MIN_ARGUMENTS).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        List<String> channels = context.getSlice(1, context.getArgCount());
        var clientChannel = context.getClientChannel();
        var pubSubManager = context.getServerContext().getPubSubManager();

        if (channels.isEmpty()) {
            return CommandResult.async(); // no-op, stay in pubsub mode
        }

        List<ByteBuffer> replies = new ArrayList<>();
        for (String channel : channels) {
            pubSubManager.subscribe(clientChannel, List.of(channel));
            int subscriptionCount = pubSubManager.subscriptionCount(clientChannel);

            var ack = ResponseBuilder.arrayOfBuffers(List.of(
                    ResponseBuilder.bulkString("subscribe"),
                    ResponseBuilder.bulkString(channel),
                    ResponseBuilder.integer(subscriptionCount)));

            replies.add(ack);
        }

        return CommandResult.success(replies);
    }

}
