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

public class ReplconfCommand extends ReplicationCommand {
    private static final Logger log = LoggerFactory.getLogger(ReplconfCommand.class);

    private final Map<String, ReplconfHandler> handlers;

    public ReplconfCommand() {
        this.handlers = createHandlers();
    }

    @Override
    public String getName() {
        return "REPLCONF";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        ValidationResult result = CommandValidator.validateMinArgs(context, 3);
        if (!result.isValid()) {
            return result;
        }

        if (context.getArgCount() % 2 != 1) {
            return ValidationResult.invalid("REPLCONF requires key-value pairs");
        }

        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        Map<String, String> params = context.getFieldValueMap(1);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();

            ReplconfHandler handler = handlers.getOrDefault(key, ReplconfHandler.unknown());
            CommandResult result = handler.handle(key, value, context);

            if (result != null && result.isError()) {
                return result;
            }
        }

        return CommandResult.success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
    }

    private Map<String, ReplconfHandler> createHandlers() {
        Map<String, ReplconfHandler> handlers = new HashMap<>();
        handlers.put("listening-port", ReplconfHandler.listeningPort());
        handlers.put("capa", ReplconfHandler.capability());
        handlers.put("ack", ReplconfHandler.ack());
        handlers.put("getack", ReplconfHandler.getAck());
        return handlers;
    }
}