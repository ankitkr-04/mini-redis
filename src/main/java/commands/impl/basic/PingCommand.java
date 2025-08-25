package commands.impl.basic;

import java.nio.ByteBuffer;
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
        boolean inPubSubMode = context.getServerContext()
                .getPubSubManager()
                .isInPubSubMode(context.getClientChannel());

        String message = (context.getArgs().length == 1) ? null : context.getArgs()[1];

        ByteBuffer response;
        if (inPubSubMode) {
            // Pub/Sub mode → always array reply: ["pong", <message or "">]
            String second = (message != null) ? message : "";
            response = ResponseBuilder.array(List.of(
                    ProtocolConstants.PONG_RESPONSE.toLowerCase(),
                    second));
        } else if (message == null) {
            // No argument → simple string "PONG"
            response = ResponseBuilder.simpleString(ProtocolConstants.PONG_RESPONSE);
        } else {
            // With argument → bulk string reply of argument
            response = ResponseBuilder.bulkString(message);
        }

        return CommandResult.success(response);
    }
}
