package commands.base;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.context.CommandContext;
import commands.core.Command;
import commands.result.CommandResult;
import commands.validation.ValidationResult;

/**
 * Provides a base implementation for Redis protocol commands.
 * Handles validation, execution timing, and error tracking.
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommand.class);

    /**
     * Executes the command with validation and metrics collection.
     * Logs errors and tracks execution time.
     *
     * @param context the command context
     * @return the result of command execution
     */
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

            Duration duration = Duration.between(start, Instant.now());
            metricsCollector.recordCommandExecution(getName(), duration);

            LOGGER.debug("Executed command: {} in {} ms", getName(), duration.toMillis());
            return result;
        } catch (Exception e) {
            LOGGER.error("Command execution failed: {}", e.getMessage(), e);
            metricsCollector.incrementTotalErrors();
            metricsCollector.incrementCommandError(getName());
            return CommandResult.error(e.getMessage());
        }
    }

    /**
     * Validates the command context.
     *
     * @param context the command context
     * @return true if valid, false otherwise
     */
    @Override
    public final boolean validate(CommandContext context) {
        return performValidation(context).isValid();
    }

    /**
     * Performs command-specific validation.
     *
     * @param context the command context
     * @return the validation result
     */
    protected abstract ValidationResult performValidation(CommandContext context);

    /**
     * Executes the command logic.
     *
     * @param context the command context
     * @return the result of command execution
     */
    protected abstract CommandResult executeInternal(CommandContext context);

}