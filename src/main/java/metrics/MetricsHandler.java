package metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles metrics requests and formats metrics output for various formats such
 * as Redis INFO and Prometheus.
 * Provides methods to retrieve metrics for specific sections or all metrics.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class MetricsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsHandler.class);

    // Section headers
    private static final String SECTION_SERVER = "# Server\r\n";
    private static final String SECTION_ENDPOINT = "# Endpoint\r\n";
    private static final String SECTION_KEYSPACE = "# Keyspace\r\n";
    private static final String SECTION_COMMANDS = "# Commands\r\n";
    private static final String SECTION_STORAGE = "# Storage\r\n";
    private static final String SECTION_REPLICATION = "# Replication\r\n";
    private static final String SECTION_PUBSUB = "# PubSub\r\n";
    private static final String SECTION_TRANSACTIONS = "# Transactions\r\n";
    private static final String SECTION_PERSISTENCE = "# Persistence\r\n";
    private static final String LINE_SEPARATOR = "\r\n";

    // Metric keys
    private static final String KEY_ACTIVE_CONNECTIONS = "active_connections";
    private static final String KEY_TOTAL_COMMANDS_PROCESSED = "total_commands_processed";
    private static final String KEY_TOTAL_ERRORS = "total_errors";
    private static final String KEY_MEMORY_USAGE_BYTES = "memory_usage_bytes";
    private static final String KEY_UPTIME_SECONDS = "uptime_seconds";
    private static final String KEY_COMMANDS = "commands";
    private static final String KEY_STORAGE = "storage";
    private static final String KEY_REPLICATION = "replication";
    private static final String KEY_PUBSUB = "pubsub";
    private static final String KEY_TRANSACTIONS = "transactions";
    private static final String KEY_PERSISTENCE = "persistence";

    private final MetricsCollector metricsCollector;

    public MetricsHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * Returns the underlying metrics collector.
     * 
     * @return the metrics collector instance
     */
    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    /**
     * Returns all metrics formatted as a Redis INFO-style output.
     * 
     * @return Formatted metrics string
     */
    public String getMetricsAsInfo() {
        Map<String, Object> allMetrics = metricsCollector.getAllMetrics();
        StringBuilder infoBuilder = new StringBuilder();

        // Server metrics section
        infoBuilder.append(SECTION_SERVER);
        infoBuilder.append(KEY_ACTIVE_CONNECTIONS).append(":").append(allMetrics.get(KEY_ACTIVE_CONNECTIONS))
                .append(LINE_SEPARATOR);
        infoBuilder.append(KEY_TOTAL_COMMANDS_PROCESSED).append(":")
                .append(allMetrics.get(KEY_TOTAL_COMMANDS_PROCESSED)).append(LINE_SEPARATOR);
        infoBuilder.append(KEY_TOTAL_ERRORS).append(":").append(allMetrics.get(KEY_TOTAL_ERRORS))
                .append(LINE_SEPARATOR);
        infoBuilder.append(KEY_MEMORY_USAGE_BYTES).append(":").append(allMetrics.get(KEY_MEMORY_USAGE_BYTES))
                .append(LINE_SEPARATOR);
        infoBuilder.append(KEY_UPTIME_SECONDS).append(":")
                .append(String.format("%.0f", (Double) allMetrics.get(KEY_UPTIME_SECONDS))).append(LINE_SEPARATOR);
        infoBuilder.append("blocked_clients:").append(metricsCollector.getBlockedClients()).append(LINE_SEPARATOR);
        infoBuilder.append("total_net_input_bytes:")
                .append(String.format("%.0f", metricsCollector.getTotalNetInputBytes())).append(LINE_SEPARATOR);
        infoBuilder.append("total_net_output_bytes:")
                .append(String.format("%.0f", metricsCollector.getTotalNetOutputBytes())).append(LINE_SEPARATOR);
        infoBuilder.append("total_connections_received:")
                .append(String.format("%.0f", metricsCollector.getTotalConnectionsReceived())).append(LINE_SEPARATOR);
        infoBuilder.append(LINE_SEPARATOR);

        // Endpoint metrics section
        infoBuilder.append(SECTION_ENDPOINT);
        infoBuilder.append("client_connections:").append(String.format("%.0f", metricsCollector.getClientConnections()))
                .append(LINE_SEPARATOR);
        infoBuilder.append("client_disconnections:")
                .append(String.format("%.0f", metricsCollector.getClientDisconnections())).append(LINE_SEPARATOR);
        infoBuilder.append("client_establishment_failures:")
                .append(String.format("%.0f", metricsCollector.getClientConnectionFailures())).append(LINE_SEPARATOR);
        infoBuilder.append("read_requests:").append(String.format("%.0f", metricsCollector.getReadRequests()))
                .append(LINE_SEPARATOR);
        infoBuilder.append("write_requests:").append(String.format("%.0f", metricsCollector.getWriteRequests()))
                .append(LINE_SEPARATOR);
        infoBuilder.append("other_requests:").append(String.format("%.0f", metricsCollector.getOtherRequests()))
                .append(LINE_SEPARATOR);
        infoBuilder.append("read_responses:").append(String.format("%.0f", metricsCollector.getReadResponses()))
                .append(LINE_SEPARATOR);
        infoBuilder.append("write_responses:").append(String.format("%.0f", metricsCollector.getWriteResponses()))
                .append(LINE_SEPARATOR);
        infoBuilder.append("other_responses:").append(String.format("%.0f", metricsCollector.getOtherResponses()))
                .append(LINE_SEPARATOR);
        infoBuilder.append(LINE_SEPARATOR);

        // Keyspace metrics section
        infoBuilder.append(SECTION_KEYSPACE);
        infoBuilder.append("total_keys:").append(metricsCollector.getTotalKeys()).append(LINE_SEPARATOR);
        infoBuilder.append("string_keys:").append(metricsCollector.getStringKeys()).append(LINE_SEPARATOR);
        infoBuilder.append("list_keys:").append(metricsCollector.getListKeys()).append(LINE_SEPARATOR);
        infoBuilder.append("set_keys:").append(metricsCollector.getSetKeys()).append(LINE_SEPARATOR);
        infoBuilder.append("zset_keys:").append(metricsCollector.getZsetKeys()).append(LINE_SEPARATOR);
        infoBuilder.append("hash_keys:").append(metricsCollector.getHashKeys()).append(LINE_SEPARATOR);
        infoBuilder.append("stream_keys:").append(metricsCollector.getStreamKeys()).append(LINE_SEPARATOR);
        infoBuilder.append("keyspace_read_hits:").append(String.format("%.0f", metricsCollector.getKeyspaceReadHits()))
                .append(LINE_SEPARATOR);
        infoBuilder.append("keyspace_read_misses:")
                .append(String.format("%.0f", metricsCollector.getKeyspaceReadMisses())).append(LINE_SEPARATOR);
        infoBuilder.append("keyspace_write_hits:")
                .append(String.format("%.0f", metricsCollector.getKeyspaceWriteHits())).append(LINE_SEPARATOR);
        infoBuilder.append("keyspace_write_misses:")
                .append(String.format("%.0f", metricsCollector.getKeyspaceWriteMisses())).append(LINE_SEPARATOR);
        infoBuilder.append(LINE_SEPARATOR);

        // Commands metrics section
        appendSectionMetrics(infoBuilder, allMetrics, KEY_COMMANDS, SECTION_COMMANDS);

        // Storage metrics section
        appendSectionMetrics(infoBuilder, allMetrics, KEY_STORAGE, SECTION_STORAGE);

        // Replication metrics section
        appendSectionMetrics(infoBuilder, allMetrics, KEY_REPLICATION, SECTION_REPLICATION);

        // Pub/Sub metrics section
        @SuppressWarnings("unchecked")
        Map<String, Object> pubsubMetrics = (Map<String, Object>) allMetrics.get(KEY_PUBSUB);
        if (pubsubMetrics != null && !pubsubMetrics.isEmpty()) {
            infoBuilder.append(SECTION_PUBSUB);
            infoBuilder.append("active_channels:").append(pubsubMetrics.get("active_channels")).append(LINE_SEPARATOR);
            infoBuilder.append("messages_published:").append(pubsubMetrics.get("messages_published"))
                    .append(LINE_SEPARATOR);

            @SuppressWarnings("unchecked")
            Map<String, Integer> subscriberCounts = (Map<String, Integer>) pubsubMetrics.get("subscribers_per_channel");
            if (subscriberCounts != null && !subscriberCounts.isEmpty()) {
                subscriberCounts.forEach((channel, count) -> infoBuilder.append("subscribers_").append(channel)
                        .append(":").append(count).append(LINE_SEPARATOR));
            }
            infoBuilder.append(LINE_SEPARATOR);
        }

        // Transactions metrics section
        appendSectionMetrics(infoBuilder, allMetrics, KEY_TRANSACTIONS, SECTION_TRANSACTIONS);

        // Persistence metrics section
        appendSectionMetrics(infoBuilder, allMetrics, KEY_PERSISTENCE, SECTION_PERSISTENCE);

        return infoBuilder.toString();
    }

    /**
     * Appends a metrics section to the infoBuilder if present in allMetrics.
     */
    private void appendSectionMetrics(StringBuilder infoBuilder, Map<String, Object> allMetrics, String sectionKey,
            String sectionHeader) {
        @SuppressWarnings("unchecked")
        Map<String, Object> sectionMetrics = (Map<String, Object>) allMetrics.get(sectionKey);
        if (sectionMetrics != null && !sectionMetrics.isEmpty()) {
            infoBuilder.append(sectionHeader);
            sectionMetrics
                    .forEach((key, value) -> infoBuilder.append(key).append(":").append(value).append(LINE_SEPARATOR));
            infoBuilder.append(LINE_SEPARATOR);
        }
    }

    /**
     * Returns metrics for a specific section.
     * 
     * @param section Section name (server, commands, storage, replication, pubsub,
     *                transactions, persistence)
     * @return Map of metrics for the specified section
     */
    public Map<String, Object> getMetricsForSection(String section) {
        Map<String, Object> allMetrics = metricsCollector.getAllMetrics();
        Map<String, Object> sectionMetrics = new LinkedHashMap<>();

        switch (section.toLowerCase()) {
            case "server":
                sectionMetrics.put(KEY_ACTIVE_CONNECTIONS, allMetrics.get(KEY_ACTIVE_CONNECTIONS));
                sectionMetrics.put(KEY_TOTAL_COMMANDS_PROCESSED, allMetrics.get(KEY_TOTAL_COMMANDS_PROCESSED));
                sectionMetrics.put(KEY_TOTAL_ERRORS, allMetrics.get(KEY_TOTAL_ERRORS));
                sectionMetrics.put(KEY_MEMORY_USAGE_BYTES, allMetrics.get(KEY_MEMORY_USAGE_BYTES));
                sectionMetrics.put(KEY_UPTIME_SECONDS, allMetrics.get(KEY_UPTIME_SECONDS));
                break;
            case "commands":
                addSectionMetrics(allMetrics, sectionMetrics, KEY_COMMANDS);
                break;
            case "storage":
                addSectionMetrics(allMetrics, sectionMetrics, KEY_STORAGE);
                break;
            case "replication":
                addSectionMetrics(allMetrics, sectionMetrics, KEY_REPLICATION);
                break;
            case "pubsub":
                addSectionMetrics(allMetrics, sectionMetrics, KEY_PUBSUB);
                break;
            case "transactions":
                addSectionMetrics(allMetrics, sectionMetrics, KEY_TRANSACTIONS);
                break;
            case "persistence":
                addSectionMetrics(allMetrics, sectionMetrics, KEY_PERSISTENCE);
                break;
            default:
                LOGGER.warn("Unknown metrics section requested: {}", section);
                break;
        }

        return sectionMetrics;
    }

    /**
     * Adds metrics from a section in allMetrics to sectionMetrics.
     */
    @SuppressWarnings("unchecked")
    private void addSectionMetrics(Map<String, Object> allMetrics, Map<String, Object> sectionMetrics,
            String sectionKey) {
        Map<String, Object> metrics = (Map<String, Object>) allMetrics.get(sectionKey);
        if (metrics != null) {
            sectionMetrics.putAll(metrics);
        }
    }

    /**
     * Returns all metrics as a map.
     * 
     * @return Map containing all metrics
     */
    public Map<String, Object> getAllMetrics() {
        return metricsCollector.getAllMetrics();
    }

    /**
     * Returns metrics in Prometheus format.
     * 
     * @return Prometheus-formatted metrics string
     */
    public String getMetricsAsPrometheus() {
        Map<String, Object> allMetrics = metricsCollector.getAllMetrics();
        StringBuilder prometheusBuilder = new StringBuilder();

        // Basic server metrics
        appendPrometheusMetric(prometheusBuilder, "redis_connected_clients", allMetrics.get(KEY_ACTIVE_CONNECTIONS),
                "Number of client connections");
        appendPrometheusMetric(prometheusBuilder, "redis_commands_processed_total",
                allMetrics.get(KEY_TOTAL_COMMANDS_PROCESSED),
                "Total number of commands processed");
        appendPrometheusMetric(prometheusBuilder, "redis_errors_total", allMetrics.get(KEY_TOTAL_ERRORS),
                "Total number of errors");
        appendPrometheusMetric(prometheusBuilder, "redis_memory_usage_bytes", allMetrics.get(KEY_MEMORY_USAGE_BYTES),
                "Memory usage in bytes");
        appendPrometheusMetric(prometheusBuilder, "redis_uptime_seconds", allMetrics.get(KEY_UPTIME_SECONDS),
                "Server uptime in seconds");

        // Command metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> commandMetrics = (Map<String, Object>) allMetrics.get(KEY_COMMANDS);
        if (commandMetrics != null) {
            commandMetrics.forEach((metricName, metricValue) -> {
                if (metricName.endsWith("_count")) {
                    String commandName = metricName.substring(0, metricName.length() - 6);
                    appendPrometheusMetricWithTags(prometheusBuilder, "redis_command_executions_total", metricValue,
                            "Number of executions for command", "command", commandName);
                } else if (metricName.endsWith("_errors")) {
                    String commandName = metricName.substring(0, metricName.length() - 7);
                    appendPrometheusMetricWithTags(prometheusBuilder, "redis_command_errors_total", metricValue,
                            "Number of errors for command", "command", commandName);
                }
            });
        }

        // Additional metrics sections can be added here as needed

        return prometheusBuilder.toString();
    }

    /**
     * Appends a Prometheus metric line to the builder.
     */
    private void appendPrometheusMetric(StringBuilder builder, String metricName, Object value, String help) {
        builder.append("# HELP ").append(metricName).append(" ").append(help).append("\n");
        builder.append("# TYPE ").append(metricName).append(" gauge\n");
        builder.append(metricName).append(" ").append(value).append("\n");
    }

    /**
     * Appends a Prometheus metric line with tags to the builder.
     */
    private void appendPrometheusMetricWithTags(StringBuilder builder, String metricName, Object value, String help,
            String... tags) {
        builder.append("# HELP ").append(metricName).append(" ").append(help).append("\n");
        builder.append("# TYPE ").append(metricName).append(" counter\n");
        builder.append(metricName).append("{");
        for (int i = 0; i < tags.length; i += 2) {
            if (i > 0)
                builder.append(",");
            builder.append(tags[i]).append("=\"").append(tags[i + 1]).append("\"");
        }
        builder.append("} ").append(value).append("\n");
    }
}
