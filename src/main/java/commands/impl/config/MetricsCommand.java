package commands.impl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Handles the METRICS command, returning server metrics in either Prometheus or
 * info format.
 * 
 * Usage: METRICS [prometheus|info]
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class MetricsCommand extends ReadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCommand.class);

    private static final String COMMAND_NAME = "METRICS";
    private static final String FORMAT_PROMETHEUS = "prometheus";
    private static final String FORMAT_INFO = "info";
    private static final int MIN_ARGS = 1;
    private static final int MAX_ARGS = 2;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argRange(MIN_ARGS, MAX_ARGS).validate(context);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String[] args = context.getArgs();
        var metricsHandler = context.getServerContext().getMetricsHandler();

        if (metricsHandler == null) {
            LOGGER.warn("Metrics handler not available in server context.");
            return CommandResult.error("Metrics not available");
        }

        String format = FORMAT_INFO;
        if (args.length > 1) {
            format = args[1].toLowerCase();
        }

        String metricsOutput;
        if (FORMAT_PROMETHEUS.equals(format)) {
            LOGGER.debug("Returning metrics in Prometheus format.");
            metricsOutput = metricsHandler.getMetricsAsPrometheus();
        } else {
            LOGGER.debug("Returning metrics in info format.");
            metricsOutput = metricsHandler.getMetricsAsInfo();
        }

        return CommandResult.success(ResponseBuilder.bulkString(metricsOutput));
    }
}
