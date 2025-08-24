package commands.impl.basic;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

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
        return CommandResult.success(ResponseBuilder.map(info));
    }

    private Map<String, String> getInfo(String section, CommandContext context) {
        Map<String, String> info = new LinkedHashMap<>();

        // If no section is specified or "all" is requested, include all sections
        if (section == null || section.equals("all")) {
            info.putAll(getServerInfo(context));
            info.putAll(getReplicationInfo(context));
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
}