package commands.impl.config;

import java.util.LinkedHashMap;
import java.util.Map;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

public final class InfoCommand extends ReadCommand {
    @Override
    public String getName() {
        return "INFO";
    }

    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.validateArgRange(context, 1, 2);
    }

    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String section = context.getArgCount() == 2 ? context.getArg(1).toLowerCase() : null;
        Map<String, String> info = getInfo(section, context);

        // Format as a bulk string with key:value pairs separated by \r\n
        StringBuilder infoText = new StringBuilder();
        for (Map.Entry<String, String> entry : info.entrySet()) {
            infoText.append(entry.getKey()).append(":").append(entry.getValue()).append("\r\n");
        }

        return CommandResult.success(ResponseBuilder.bulkString(infoText.toString()));
    }

    private Map<String, String> getInfo(String section, CommandContext context) {
        Map<String, String> info = new LinkedHashMap<>();

        // If no section is specified or "all" is requested, include all sections
        if (section == null || section.equals("all")) {
            info.putAll(getServerInfo(context));
            info.putAll(getReplicationInfo(context));
            info.putAll(getMetricsInfo(context));
            // Add more sections here as needed (e.g., memory, clients)
        } else {
            // Handle specific sections
            switch (section) {
                case "server":
                    info.putAll(getServerInfo(context));
                    break;
                case "replication":
                    info.putAll(getReplicationInfo(context));
                    break;
                case "metrics":
                    info.putAll(getMetricsInfo(context));
                    break;
                // Add more cases for additional sections
                default:
                    // Return empty map for unknown sections (or could return error)
                    break;
            }
        }

        return info;
    }

    private Map<String, String> getServerInfo(CommandContext context) {
        var config = context.getServerContext().getConfig();
        Map<String, String> serverInfo = new LinkedHashMap<>();
        serverInfo.put("port", String.valueOf(config.port()));
        serverInfo.put("bind_address", config.bindAddress());
        serverInfo.put("data_directory", config.dataDirectory());
        serverInfo.put("database_filename", config.databaseFilename());
        serverInfo.put("append_only_mode", String.valueOf(config.appendOnlyMode() ? 1 : 0));
        serverInfo.put("max_memory", String.valueOf(config.maxMemory()));
        serverInfo.put("require_password", config.requirePassword().isPresent() ? "yes" : "no");
        return serverInfo;
    }

    private Map<String, String> getReplicationInfo(CommandContext context) {
        return context.getServerContext().getReplicationState().toInfoMap();
    }
    
    private Map<String, String> getMetricsInfo(CommandContext context) {
        var metricsHandler = context.getServerContext().getMetricsHandler();
        Map<String, String> metricsInfo = new LinkedHashMap<>();
        
        if (metricsHandler != null) {
            Map<String, Object> allMetrics = metricsHandler.getAllMetrics();
            
            // Convert metrics to string format for INFO output
            metricsInfo.put("active_connections", String.valueOf(allMetrics.get("active_connections")));
            metricsInfo.put("total_commands_processed", String.valueOf(allMetrics.get("total_commands_processed")));
            metricsInfo.put("total_errors", String.valueOf(allMetrics.get("total_errors")));
            metricsInfo.put("memory_usage_bytes", String.valueOf(allMetrics.get("memory_usage_bytes")));
            
            Double uptimeSeconds = (Double) allMetrics.get("uptime_seconds");
            metricsInfo.put("uptime_seconds", String.format("%.0f", uptimeSeconds != null ? uptimeSeconds : 0.0));
        }
        
        return metricsInfo;
    }
}