package commands.impl.replication;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.BaseCommand;
import config.ProtocolConstants;
import core.ServerContext;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public class ReplconfCommand extends BaseCommand {
    private static final Logger log = LoggerFactory.getLogger(ReplconfCommand.class);

    private final ServerContext context;
    private final Map<String, ReplconfHandler> handlers;

    public ReplconfCommand(ServerContext context) {
        this.context = context;
        this.handlers = createHandlers();
    }

    @Override
    public String name() {
        return "REPLCONF";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        ValidationResult result = ValidationUtils.validateMinArgs(args, 3);
        if (!result.isValid()) {
            return result;
        }

        if (args.argCount() % 2 != 1) {
            return ValidationResult.invalid("REPLCONF requires key-value pairs");
        }

        return ValidationResult.valid();
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        Map<String, String> params = args.fieldValueMap(1);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();

            ReplconfHandler handler = handlers.getOrDefault(key, ReplconfHandler.unknown());
            CommandResult result = handler.handle(key, value, args, context);

            if (result != null && !result.isSuccess()) {
                return result;
            }
        }

        return new CommandResult.Success(ResponseBuilder.encode(ProtocolConstants.RESP_OK));
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
