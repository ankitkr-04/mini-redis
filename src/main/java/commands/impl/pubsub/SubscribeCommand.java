package commands.impl.pubsub;

import java.util.List;

import commands.base.PubSubCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public class SubscribeCommand extends PubSubCommand {

    @Override
    public String getName() {
        return "SUBSCRIBE";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // need at least command name + 1 channel/pattern
        return CommandValidator.validateMinArgs(context, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        boolean isPatternBased = "PSUBSCRIBE".equalsIgnoreCase(context.getOperation());
        List<String> targets = context.getSlice(1, context.getArgCount());

        var manager = context.getServerContext().getPubSubManager();
        var client = context.getClientChannel();

        if (isPatternBased) {
            manager.psubscribe(client, targets);
        } else {
            manager.subscribe(client, targets);
        }

        // Acknowledge each subscription separately
        for (String target : targets) {
            String kind = isPatternBased ? "psubscribe" : "subscribe";
            int count = manager.subscriptionCount(client);

            var ack = ResponseBuilder.arrayOfBuffers(List.of(
                    ResponseBuilder.bulkString(kind),
                    ResponseBuilder.bulkString(target),
                    ResponseBuilder.integer(count)));

            return CommandResult.success(ack);
        }

        return CommandResult.async();
    }
}