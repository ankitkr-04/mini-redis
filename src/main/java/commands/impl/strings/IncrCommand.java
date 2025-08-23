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
            long newValue = storage.incrementString(key); // now returns long
            publishDataAdded(key);
            return new CommandResult.Success(ResponseBuilder.integer(newValue));
        } catch (IllegalStateException e) {
            // WRONG TYPE
            return new CommandResult.Error(e.getMessage());
        } catch (NumberFormatException e) {
            // NOT AN INTEGER or overflow
            return new CommandResult.Error(e.getMessage());
        } catch (Exception e) {
            // catch-all to avoid leaking stack traces as strange RESP replies
            return new CommandResult.Error(e.getMessage());
        }
    }


}
