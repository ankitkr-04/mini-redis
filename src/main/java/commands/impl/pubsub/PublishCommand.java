package commands.impl.pubsub;

import commands.base.PubSubCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.ValidationResult;

public class PublishCommand extends PubSubCommand {

    @Override
    public String getName() {
        return "PUBLISH";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'performValidation'");
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeInternal'");
    }

}
