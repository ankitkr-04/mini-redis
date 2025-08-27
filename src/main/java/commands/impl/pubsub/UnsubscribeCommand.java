package commands.impl.pubsub;

import java.nio.ByteBuffer;
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
 * Handles the UNSUBSCRIBE and PUNSUBSCRIBE commands for Redis protocol.
 * Unsubscribes the client from one or more channels or patterns.
 * If no channels/patterns are specified, unsubscribes from all.
 */
public class UnsubscribeCommand extends PubSubCommand {

    private static final Logger logger = LoggerFactory.getLogger(UnsubscribeCommand.class);

    private static final String UNSUBSCRIBE_COMMAND = "UNSUBSCRIBE";
    private static final String PUNSUBSCRIBE_COMMAND = "PUNSUBSCRIBE";
    private static final String UNSUBSCRIBE_KIND = "unsubscribe";
    private static final String PUNSUBSCRIBE_KIND = "punsubscribe";
    private static final int MIN_ARG_COUNT = 1; // At least command name

    @Override
    public String getName() {
        return UNSUBSCRIBE_COMMAND;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Accepts 0 or more arguments (command name always present)
        return CommandValidator.minArgs(MIN_ARG_COUNT).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        boolean isPatternUnsubscribe = PUNSUBSCRIBE_COMMAND.equalsIgnoreCase(context.getOperation());
        var pubSubManager = context.getServerContext().getPubSubManager();
        var clientChannel = context.getClientChannel();

        // If no arguments, unsubscribe from all; otherwise, unsubscribe from specified
        // targets
        List<String> unsubscribeTargets;

        if (context.getArgCount() > 1) {
            unsubscribeTargets = context.getSlice(1, context.getArgCount());
        } else {
            unsubscribeTargets = null;
        }

        if (isPatternUnsubscribe) {
            pubSubManager.punsubscribe(clientChannel, unsubscribeTargets);
        } else {
            pubSubManager.unsubscribe(clientChannel, unsubscribeTargets);
        }

        // Determine which channels/patterns were unsubscribed for acknowledgement
        List<String> unsubscribedList = (unsubscribeTargets != null && !unsubscribeTargets.isEmpty())
                ? unsubscribeTargets
                : (isPatternUnsubscribe
                        ? List.copyOf(pubSubManager.getOrCreateState(clientChannel).getSubscribedPatterns())
                        : List.copyOf(pubSubManager.getOrCreateState(clientChannel).getSubscribedChannels()));

        // Build responses for all unsubscribed targets
        List<ByteBuffer> responses = new java.util.ArrayList<>();
        for (String unsubscribed : unsubscribedList) {
            String responseKind = isPatternUnsubscribe ? PUNSUBSCRIBE_KIND : UNSUBSCRIBE_KIND;
            int remainingSubscriptions = pubSubManager.subscriptionCount(clientChannel);

            var response = ResponseBuilder.arrayOfBuffers(List.of(
                    ResponseBuilder.bulkString(responseKind),
                    ResponseBuilder.bulkString(unsubscribed),
                    ResponseBuilder.integer(remainingSubscriptions)));

            logger.debug("Client {} unsubscribed from {}: {}", clientChannel, responseKind, unsubscribed);

            responses.add(response);
        }

        // Return all at once using multi-success
        return responses.isEmpty()
                ? CommandResult.async()
                : CommandResult.success(responses);
    }
}
