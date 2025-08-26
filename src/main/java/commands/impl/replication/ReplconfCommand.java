package commands.impl.replication;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReplicationCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import config.ProtocolConstants;
import protocol.ResponseBuilder;

/**
 * Handles the Redis REPLCONF command for replication configuration.
 * 
 * Supports subcommands such as LISTENING-PORT, CAPA, ACK, and GETACK.
 * Validates that arguments are provided as key-value pairs.
 * Delegates handling to specific subcommand handlers.
 */
public class ReplconfCommand extends ReplicationCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplconfCommand.class);

    private static final String COMMAND_NAME = "REPLCONF";
    private static final int MIN_ARGUMENTS = 3;
    private static final String ERROR_KEY_VALUE_PAIRS = "REPLCONF requires key-value pairs";

    private final Map<String, ReplconfHandler> replconfHandlers;

    public ReplconfCommand() {
        this.replconfHandlers = initializeHandlers();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.minArgs(MIN_ARGUMENTS).and(CommandValidator.oddArgCount()).validate(context);

    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        Map<String, String> argumentPairs = context.getFieldValueMap(1);

        for (Map.Entry<String, String> argument : argumentPairs.entrySet()) {
            String subcommand = argument.getKey().toLowerCase();
            String subcommandValue = argument.getValue();

            ReplconfHandler handler = replconfHandlers.getOrDefault(subcommand, ReplconfHandler.unknown());
            CommandResult handlerResult = handler.handle(subcommand, subcommandValue, context);

            if (handlerResult != null) {
                if (handlerResult.isError()) {
                    LOGGER.warn("REPLCONF subcommand '{}' failed", subcommand);
                    return handlerResult;
                }
                return handlerResult;
            }
        }

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }

    private Map<String, ReplconfHandler> initializeHandlers() {
        Map<String, ReplconfHandler> handlers = new HashMap<>();
        handlers.put("listening-port", ReplconfHandler.listeningPort());
        handlers.put("capa", ReplconfHandler.capability());
        handlers.put("ack", ReplconfHandler.ack());
        handlers.put("getack", ReplconfHandler.getAck());
        return handlers;
    }
}
