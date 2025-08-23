package commands.impl.lists;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.WriteCommand;
import events.StorageEventPublisher;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.CommandValidator;
import validation.ValidationResult;

public final class PushCommand extends WriteCommand {
    public PushCommand(StorageEventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public String name() {
        return "PUSH"; // Handles both LPUSH and RPUSH
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return CommandValidator.validateArgRange(args, 3, Integer.MAX_VALUE);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String operation = args.operation().toUpperCase();
        String key = args.key();
        String[] values = args.values();

        int newSize = switch (operation) {
            case "LPUSH" -> storage.leftPush(key, values);
            case "RPUSH" -> storage.rightPush(key, values);
            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };

        publishDataAdded(key);

        return new CommandResult.Success(ResponseBuilder.integer(newSize));
    }
}
