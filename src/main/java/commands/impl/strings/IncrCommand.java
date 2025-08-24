package commands.impl.strings;

import commands.CommandArgs;
import commands.CommandResult;
import commands.base.WriteCommand;
import events.StorageEventPublisher;
import protocol.ResponseBuilder;
import storage.StorageService;
import validation.ValidationResult;
import validation.ValidationUtils;

public class IncrCommand extends WriteCommand {

    public IncrCommand(StorageEventPublisher eventPublisher) {
        super(eventPublisher);
    }

    @Override
    public String name() {
        return "INCR";
    }

    @Override
    protected ValidationResult validateCommand(CommandArgs args) {
        return ValidationUtils.validateArgCount(args, 2);
    }

    @Override
    protected CommandResult executeCommand(CommandArgs args, StorageService storage) {
        String key = args.key();
        try {
            long newValue = storage.incrementString(key);
            publishDataAdded(key);

            // Propagate to replicas
            propagateCommand(args.rawArgs());

            return new CommandResult.Success(ResponseBuilder.integer(newValue));
        } catch (IllegalStateException | NumberFormatException e) {
            return new CommandResult.Error(e.getMessage());
        } catch (Exception e) {
            return new CommandResult.Error(e.getMessage());
        }
    }
}