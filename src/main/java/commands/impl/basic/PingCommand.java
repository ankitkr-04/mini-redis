package commands.impl.basic;

import java.util.List;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

public final class PingCommand extends ReadCommand {
    @Override
    public String getName() {
        return "PING";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgRange(context, 1, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        boolean isInPubSubMode = context.getServerContext().getPubSubManager()
                .isInPubSubMode(context.getClientChannel());

        String response;
        if (context.getArgs().length == 1) {
            response = ProtocolConstants.PONG_RESPONSE; // "PONG"
        } else {
            response = context.getArgs()[1]; // echo back message
        }

        var toSend = isInPubSubMode
                ? ResponseBuilder.array(List.of(ProtocolConstants.PONG_RESPONSE, response))
                : ResponseBuilder.bulkString(response);

        return CommandResult.success(toSend);
    }

}