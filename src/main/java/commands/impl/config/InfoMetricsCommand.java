package commands.impl.config;

import java.util.Map;
import java.util.LinkedHashMap;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implementation of the INFO METRICS command.
 * Returns server metrics in Redis INFO format.
 */
public final class InfoMetricsCommand extends ReadCommand {

    @Override
    public String getName() {
        return "INFO_METRICS";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgRange(context, 2, 3);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String[] args = context.getArgs();
        
        // Get metrics handler from server context
        var metricsHandler = context.getServerContext().getMetricsHandler();
        if (metricsHandler == null) {
            return CommandResult.error("Metrics not available");
        }
        
        // Check if a specific section is requested
        String section = null;
        if (args.length > 2) {
            section = args[2].toLowerCase();
        }
        
        String metricsOutput;
        if (section != null && !section.equals("all")) {
            // Get specific section metrics
            Map<String, Object> sectionMetrics = metricsHandler.getMetricsForSection(section);
            if (sectionMetrics.isEmpty()) {
                return CommandResult.error("Unknown metrics section: " + section);
            }
            metricsOutput = formatSectionMetrics(section, sectionMetrics);
        } else {
            // Get all metrics
            metricsOutput = metricsHandler.getMetricsAsInfo();
        }
        
        return CommandResult.success(ResponseBuilder.bulkString(metricsOutput));
    }
    
    private String formatSectionMetrics(String sectionName, Map<String, Object> metrics) {
        StringBuilder result = new StringBuilder();
        result.append("# ").append(sectionName.toUpperCase()).append("\r\n");
        
        metrics.forEach((key, value) -> {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMetrics = (Map<String, Object>) value;
                subMetrics.forEach((subKey, subValue) -> 
                    result.append(subKey).append(":").append(subValue).append("\r\n"));
            } else {
                result.append(key).append(":").append(value).append("\r\n");
            }
        });
        
        return result.toString();
    }
}
