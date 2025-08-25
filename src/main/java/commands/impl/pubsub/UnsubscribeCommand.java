package commands.impl.pubsub;

import java.util.List;

import commands.base.PubSubCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class UnsubscribeCommand extends PubSubCommand {

    @Override
    public String getName() {
        return "UNSUBSCRIBE";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // UNSUBSCRIBE/PUNSUBSCRIBE can be called with 0 or more channels/patterns
        // so just ensure command name exists
        return CommandValidator.validateMinArgs(context, 1);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        boolean isPatternBased = "PUNSUBSCRIBE".equalsIgnoreCase(context.getOperation());
        var manager = context.getServerContext().getPubSubManager();
        var client = context.getClientChannel();

        // If no arguments, Redis unsubscribes from all
        List<String> targets = context.getArgCount() > 1
                ? context.getSlice(1, context.getArgCount())
                : null;

        if (isPatternBased) {
            manager.punsubscribe(client, targets);
        } else {
            manager.unsubscribe(client, targets);
        }

        // For acknowledgements, figure out which keys were removed
        List<String> unsubscribed = (targets != null && !targets.isEmpty())
                ? targets
                : (isPatternBased
                        ? List.copyOf(manager.getOrCreateState(client).getSubscribedPatterns())
                        : List.copyOf(manager.getOrCreateState(client).getSubscribedChannels()));

        for (String target : unsubscribed) {
            String kind = isPatternBased ? "punsubscribe" : "unsubscribe";
            int count = manager.subscriptionCount(client);

            var ack = ResponseBuilder.arrayOfBuffers(List.of(
                    ResponseBuilder.bulkString(kind),
                    ResponseBuilder.bulkString(target),
                    ResponseBuilder.integer(count)));
            return CommandResult.success(ack);
        }

        return CommandResult.async(); // responses sent directly
    }
}
