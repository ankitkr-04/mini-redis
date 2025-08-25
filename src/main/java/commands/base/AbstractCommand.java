package commands.base;

import java.time.Duration;
import java.time.Instant;

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

        Instant start = Instant.now();
        var metricsCollector = context.getServerContext().getMetricsCollector();
        
        try {
            metricsCollector.incrementTotalCommands();
            CommandResult result = executeInternal(context);
            
            // Record command execution metrics
            Duration duration = Duration.between(start, Instant.now());
            metricsCollector.recordCommandExecution(getName(), duration);
            
            return result;
        } catch (Exception e) {
            log.error("Command execution failed: {}", e.getMessage(), e);
            metricsCollector.incrementTotalErrors();
            metricsCollector.incrementCommandError(getName());
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