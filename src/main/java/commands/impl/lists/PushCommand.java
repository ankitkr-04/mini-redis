package commands.impl.lists;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.WriteCommand;
import events.StorageEventPublisher;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public final class PushCommand extends WriteCommand {
    public PushCommand(StorageEventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public String name() {
        return "PUSH"; // LPUSH/RPUSH
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgRange(args, 3, Integer.MAX_VALUE);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String key = args.key();
        String[] values = args.values();
        boolean isLeft = "LPUSH".equalsIgnoreCase(args.operation());
        int newSize = isLeft ? storage.leftPush(key, values) : storage.rightPush(key, values);
        publishDataAdded(key);
        return new CommandResult.Success(ResponseBuilder.integer(newSize));
    }
}
