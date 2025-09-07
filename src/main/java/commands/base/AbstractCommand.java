package commands.base;

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
    public final CommandResult execute(final CommandContext context) {
        final ValidationResult validation = performValidation(context);
        if (!validation.isValid()) {
            return CommandResult.error(validation.getErrorMessage());
        }

        final var metricsCollector = context.getServerContext().getMetricsCollector();

        try {
            metricsCollector.incrementTotalCommands();
            final CommandResult result = executeInternal(context);

            LOGGER.debug("Executed command: {}", getName());
            return result;
        } catch (final Exception e) {
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
    public final boolean validate(final CommandContext context) {
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