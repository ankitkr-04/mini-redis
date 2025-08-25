package commands.impl.basic;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;

public class ConfigCommand extends ReadCommand {
    @Override
    public String getName() {
        return "CONFIG";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgRange(context, 1, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeInternal'");
    }

}
