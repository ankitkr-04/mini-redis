package commands.impl.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import commands.base.ReadCommand;
import commands.context.CommandContext;
import commands.result.CommandResult;
import commands.validation.CommandValidator;
import commands.validation.ValidationResult;
import protocol.ResponseBuilder;

/**
 * Implements the Redis INFO command, providing server, replication, and metrics
 * information.
 * Supports optional section argument to filter output.
 */
public final class InfoCommand extends ReadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoCommand.class);

    // Section names for INFO command
    private static final String SECTION_ALL = "all";
    private static final String SECTION_SERVER = "server";
    private static final String SECTION_REPLICATION = "replication";
    private static final String SECTION_METRICS = "metrics";

    // Info keys
    private static final String KEY_PORT = "port";
    private static final String KEY_BIND_ADDRESS = "bind_address";
    private static final String KEY_DATA_DIRECTORY = "data_directory";
    private static final String KEY_DATABASE_FILENAME = "database_filename";
    private static final String KEY_APPEND_ONLY_MODE = "append_only_mode";
    private static final String KEY_MAX_MEMORY = "max_memory";
    private static final String KEY_REQUIRE_PASSWORD = "require_password";
    private static final String KEY_ACTIVE_CONNECTIONS = "active_connections";
    private static final String KEY_TOTAL_COMMANDS_PROCESSED = "total_commands_processed";
    private static final String KEY_TOTAL_ERRORS = "total_errors";
    private static final String KEY_MEMORY_USAGE_BYTES = "memory_usage_bytes";
    private static final String KEY_UPTIME_SECONDS = "uptime_seconds";

    @Override
    public String getName() {
        return "INFO";
    }

    /**
     * Validates argument count for INFO command (1 or 2 arguments allowed).
     */
    @Override
    protected ValidationResult performValidation(CommandContext context) {
        return CommandValidator.argRange(1, 2).validate(context);
    }

    /**
     * Executes the INFO command, returning server, replication, and metrics info.
     * If a section is specified, only that section is returned.
     */
    @Override
    protected CommandResult executeInternal(CommandContext context) {
        String section = context.getArgCount() == 2 ? context.getArg(1).toLowerCase() : null;
        Map<String, String> info = getInfo(section, context);

        StringBuilder infoText = new StringBuilder();
        for (Map.Entry<String, String> entry : info.entrySet()) {
            infoText.append(entry.getKey()).append(":").append(entry.getValue()).append("\r\n");
        }

        return CommandResult.success(ResponseBuilder.bulkString(infoText.toString()));
    }

    /**
     * Returns info map for the requested section or all sections if section is null
     * or "all".
     */
    private Map<String, String> getInfo(String section, CommandContext context) {
        Map<String, String> info = new LinkedHashMap<>();

        if (section == null || SECTION_ALL.equals(section)) {
            info.putAll(getServerInfo(context));
            info.putAll(getReplicationInfo(context));
            info.putAll(getMetricsInfo(context));
        } else {
            switch (section) {
                case SECTION_SERVER -> info.putAll(getServerInfo(context));
                case SECTION_REPLICATION -> info.putAll(getReplicationInfo(context));
                case SECTION_METRICS -> info.putAll(getMetricsInfo(context));
                default -> LOGGER.debug("Unknown INFO section requested: {}", section);
            }
        }
        return info;
    }

    /**
     * Returns server configuration info as key-value pairs.
     */
    private Map<String, String> getServerInfo(CommandContext context) {
        var config = context.getServerContext().getConfig();
        Map<String, String> serverInfo = new LinkedHashMap<>();
        serverInfo.put(KEY_PORT, String.valueOf(config.port()));
        serverInfo.put(KEY_BIND_ADDRESS, config.bindAddress());
        serverInfo.put(KEY_DATA_DIRECTORY, config.dataDirectory());
        serverInfo.put(KEY_DATABASE_FILENAME, config.databaseFilename());
        serverInfo.put(KEY_APPEND_ONLY_MODE, String.valueOf(config.appendOnlyMode() ? 1 : 0));
        serverInfo.put(KEY_MAX_MEMORY, String.valueOf(config.maxMemory()));
        serverInfo.put(KEY_REQUIRE_PASSWORD, config.requirePassword().isPresent() ? "yes" : "no");
        return serverInfo;
    }

    /**
     * Returns replication state info as key-value pairs.
     */
    private Map<String, String> getReplicationInfo(CommandContext context) {
        return context.getServerContext().getReplicationState().toInfoMap();
    }

    /**
     * Returns metrics info as key-value pairs.
     */
    private Map<String, String> getMetricsInfo(CommandContext context) {
        var metricsHandler = context.getServerContext().getMetricsHandler();
        Map<String, String> metricsInfo = new LinkedHashMap<>();

        if (metricsHandler != null) {
            Map<String, Object> allMetrics = metricsHandler.getAllMetrics();
            metricsInfo.put(KEY_ACTIVE_CONNECTIONS, String.valueOf(allMetrics.get(KEY_ACTIVE_CONNECTIONS)));
            metricsInfo.put(KEY_TOTAL_COMMANDS_PROCESSED, String.valueOf(allMetrics.get(KEY_TOTAL_COMMANDS_PROCESSED)));
            metricsInfo.put(KEY_TOTAL_ERRORS, String.valueOf(allMetrics.get(KEY_TOTAL_ERRORS)));
            metricsInfo.put(KEY_MEMORY_USAGE_BYTES, String.valueOf(allMetrics.get(KEY_MEMORY_USAGE_BYTES)));
            Double uptimeSeconds = (Double) allMetrics.get(KEY_UPTIME_SECONDS);
            metricsInfo.put(KEY_UPTIME_SECONDS, String.format("%.0f", uptimeSeconds != null ? uptimeSeconds : 0.0));
        }

        return metricsInfo;
    }
}