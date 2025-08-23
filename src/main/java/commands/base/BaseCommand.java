package commands.base;

import commands.Command;
import commands.CommandArgs;
import commands.CommandResult;
import storage.StorageService;
import validation.ValidationResult;

public abstract class BaseCommand implements Command {
    @Override
    public final CommandResult execute(CommandArgs args, StorageService storage) {
        ValidationResult validation = validateCommand(args);
        if (!validation.isValid()) {
            return new CommandResult.Error(validation.error().get().message());
        }

        try {
            return executeCommand(args, storage);
        } catch (Exception e) {
            return new CommandResult.Error(e.getMessage());
        }
    }

    protected abstract ValidationResult validateCommand(CommandArgs args);

    protected abstract CommandResult executeCommand(CommandArgs args, StorageService storage);

    @Override
    public final boolean validate(CommandArgs args) {
        return validateCommand(args).isValid();
    }
}
