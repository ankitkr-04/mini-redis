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

/**
 * Implements the Redis PING command.
 * <p>
 * Responds with a PONG message or echoes a custom message if provided.
 * In Pub/Sub mode, always replies with an array as per Redis protocol.
 * </p>
 */
public final class PingCommand extends ReadCommand {

    private static final String COMMAND_NAME = "PING";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // Accepts either "PING" or "PING <message>"
        return CommandValidator.argRange(1, 2).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        boolean inPubSubMode = context.getServerContext()
                .getPubSubManager()
                .isInPubSubMode(context.getClientChannel());

        String message = (context.getArgs().length == 1) ? null : context.getArgs()[1];

        ByteBuffer response;
        if (inPubSubMode) {
            // In Pub/Sub mode, always reply with array: ["pong", <message or "">]
            String second = (message != null) ? message : "";
            response = ResponseBuilder.array(List.of(
                    ProtocolConstants.PONG_RESPONSE.toLowerCase(),
                    second));
        } else if (message == null) {
            // No argument: simple string "PONG"
            response = ResponseBuilder.simpleString(ProtocolConstants.PONG_RESPONSE);
        } else {
            // With argument: bulk string reply of argument
            response = ResponseBuilder.bulkString(message);
        }

        return CommandResult.success(response);
    }
}
