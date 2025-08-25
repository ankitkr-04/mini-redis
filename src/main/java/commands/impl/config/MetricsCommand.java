package commands.impl.config;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implementation of the METRICS command.
 * Returns server metrics in Prometheus format.
 */
public final class MetricsCommand extends ReadCommand {

    @Override
    public String getName() {
        return "METRICS";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgRange(context, 1, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String[] args = context.getArgs();
        
        // Get metrics handler from server context
        var metricsHandler = context.getServerContext().getMetricsHandler();
        if (metricsHandler == null) {
            return CommandResult.error("Metrics not available");
        }
        
        // Check format
        String format = "info"; // default format
        if (args.length > 1) {
            format = args[1].toLowerCase();
        }
        
        String metricsOutput;
        switch (format) {
            case "prometheus":
                metricsOutput = metricsHandler.getMetricsAsPrometheus();
                break;
            case "info":
            default:
                metricsOutput = metricsHandler.getMetricsAsInfo();
                break;
        }
        
        return CommandResult.success(ResponseBuilder.bulkString(metricsOutput));
    }
}
