package commands.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.context.CommandContext;
import commands.core.Command;
import commands.result.CommandResult;
import commands.validation.ValidationResult;

public abstract class AbstractCommand implements Command {
    private static final Logger log = LoggerFactory.getLogger(AbstractCommand.class);

    @Override
    public final CommandResult execute(CommandContext context) {
        ValidationResult validation = performValidation(context);
        if (!validation.isValid()) {
            return CommandResult.error(validation.getErrorMessage());
        }

        try {
            return executeInternal(context);
        } catch (Exception e) {
            log.error("Command execution failed: {}", e.getMessage(), e);
            return CommandResult.error(e.getMessage());
        }
    }

    @Override
    public final boolean validate(CommandContext context) {
        return performValidation(context).isValid();
    }

    protected abstract ValidationResult performValidation(CommandContext context);

    protected abstract CommandResult executeInternal(CommandContext context);
}