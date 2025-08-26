package metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles metrics requests and formats metrics output.
 * Provides methods to retrieve metrics in various formats.
 */
public final class MetricsHandler {
    private static final Logger log = LoggerFactory.getLogger(MetricsHandler.class);

    private final MetricsCollector metricsCollector;

    public MetricsHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * Get the underlying metrics collector.
     * 
     * @return the metrics collector instance
     */
    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    /**
     * Get all metrics formatted as INFO-style output (Redis INFO command format).
     * Now includes Redis Enterprise compatible metrics.
     * 
     * @return Formatted metrics string
     */
    public String getMetricsAsInfo() {
        Map<String, Object> allMetrics = metricsCollector.getAllMetrics();
        StringBuilder result = new StringBuilder();

        // Server metrics section (Redis Enterprise style)
        result.append("# Server\r\n");
        result.append("active_connections:").append(allMetrics.get("active_connections")).append("\r\n");
        result.append("total_commands_processed:").append(allMetrics.get("total_commands_processed")).append("\r\n");
        result.append("total_errors:").append(allMetrics.get("total_errors")).append("\r\n");
        result.append("memory_usage_bytes:").append(allMetrics.get("memory_usage_bytes")).append("\r\n");
        result.append("uptime_seconds:").append(String.format("%.0f", (Double) allMetrics.get("uptime_seconds")))
                .append("\r\n");
        result.append("blocked_clients:").append(metricsCollector.getBlockedClients()).append("\r\n");
        result.append("total_net_input_bytes:").append(String.format("%.0f", metricsCollector.getTotalNetInputBytes()))
                .append("\r\n");
        result.append("total_net_output_bytes:")
                .append(String.format("%.0f", metricsCollector.getTotalNetOutputBytes())).append("\r\n");
        result.append("total_connections_received:")
                .append(String.format("%.0f", metricsCollector.getTotalConnectionsReceived())).append("\r\n");
        result.append("\r\n");

        // Redis Enterprise endpoint metrics
        result.append("# Endpoint\r\n");
        result.append("client_connections:").append(String.format("%.0f", metricsCollector.getClientConnections()))
                .append("\r\n");
        result.append("client_disconnections:")
                .append(String.format("%.0f", metricsCollector.getClientDisconnections())).append("\r\n");
        result.append("client_establishment_failures:")
                .append(String.format("%.0f", metricsCollector.getClientConnectionFailures())).append("\r\n");
        result.append("read_requests:").append(String.format("%.0f", metricsCollector.getReadRequests()))
                .append("\r\n");
        result.append("write_requests:").append(String.format("%.0f", metricsCollector.getWriteRequests()))
                .append("\r\n");
        result.append("other_requests:").append(String.format("%.0f", metricsCollector.getOtherRequests()))
                .append("\r\n");
        result.append("read_responses:").append(String.format("%.0f", metricsCollector.getReadResponses()))
                .append("\r\n");
        result.append("write_responses:").append(String.format("%.0f", metricsCollector.getWriteResponses()))
                .append("\r\n");
        result.append("other_responses:").append(String.format("%.0f", metricsCollector.getOtherResponses()))
                .append("\r\n");
        result.append("\r\n");

        // Keyspace metrics (Redis Enterprise style)
        result.append("# Keyspace\r\n");
        result.append("total_keys:").append(metricsCollector.getTotalKeys()).append("\r\n");
        result.append("string_keys:").append(metricsCollector.getStringKeys()).append("\r\n");
        result.append("list_keys:").append(metricsCollector.getListKeys()).append("\r\n");
        result.append("set_keys:").append(metricsCollector.getSetKeys()).append("\r\n");
        result.append("zset_keys:").append(metricsCollector.getZsetKeys()).append("\r\n");
        result.append("hash_keys:").append(metricsCollector.getHashKeys()).append("\r\n");
        result.append("stream_keys:").append(metricsCollector.getStreamKeys()).append("\r\n");
        result.append("keyspace_read_hits:").append(String.format("%.0f", metricsCollector.getKeyspaceReadHits()))
                .append("\r\n");
        result.append("keyspace_read_misses:").append(String.format("%.0f", metricsCollector.getKeyspaceReadMisses()))
                .append("\r\n");
        result.append("keyspace_write_hits:").append(String.format("%.0f", metricsCollector.getKeyspaceWriteHits()))
                .append("\r\n");
        result.append("keyspace_write_misses:").append(String.format("%.0f", metricsCollector.getKeyspaceWriteMisses()))
                .append("\r\n");
        result.append("\r\n");

        // Command metrics section
        @SuppressWarnings("unchecked")
        Map<String, Object> commandMetrics = (Map<String, Object>) allMetrics.get("commands");
        if (commandMetrics != null && !commandMetrics.isEmpty()) {
            result.append("# Commands\r\n");
            commandMetrics.forEach((key, value) -> result.append(key).append(":").append(value).append("\r\n"));
            result.append("\r\n");
        }

        // Storage metrics section
        @SuppressWarnings("unchecked")
        Map<String, Object> storageMetrics = (Map<String, Object>) allMetrics.get("storage");
        if (storageMetrics != null && !storageMetrics.isEmpty()) {
            result.append("# Storage\r\n");
            storageMetrics.forEach((key, value) -> result.append(key).append(":").append(value).append("\r\n"));
            result.append("\r\n");
        }

        // Replication metrics section
        @SuppressWarnings("unchecked")
        Map<String, Object> replicationMetrics = (Map<String, Object>) allMetrics.get("replication");
        if (replicationMetrics != null && !replicationMetrics.isEmpty()) {
            result.append("# Replication\r\n");
            replicationMetrics.forEach((key, value) -> result.append(key).append(":").append(value).append("\r\n"));
            result.append("\r\n");
        }

        // Pub/Sub metrics section
        @SuppressWarnings("unchecked")
        Map<String, Object> pubsubMetrics = (Map<String, Object>) allMetrics.get("pubsub");
        if (pubsubMetrics != null && !pubsubMetrics.isEmpty()) {
            result.append("# PubSub\r\n");
            result.append("active_channels:").append(pubsubMetrics.get("active_channels")).append("\r\n");
            result.append("messages_published:").append(pubsubMetrics.get("messages_published")).append("\r\n");

            @SuppressWarnings("unchecked")
            Map<String, Integer> subscriberCounts = (Map<String, Integer>) pubsubMetrics.get("subscribers_per_channel");
            if (subscriberCounts != null && !subscriberCounts.isEmpty()) {
                subscriberCounts.forEach((channel, count) -> result.append("subscribers_").append(channel).append(":")
                        .append(count).append("\r\n"));
            }
            result.append("\r\n");
        }

        // Transaction metrics section
        @SuppressWarnings("unchecked")
        Map<String, Object> transactionMetrics = (Map<String, Object>) allMetrics.get("transactions");
        if (transactionMetrics != null && !transactionMetrics.isEmpty()) {
            result.append("# Transactions\r\n");
            transactionMetrics.forEach((key, value) -> result.append(key).append(":").append(value).append("\r\n"));
            result.append("\r\n");
        }

        // Persistence metrics section
        @SuppressWarnings("unchecked")
        Map<String, Object> persistenceMetrics = (Map<String, Object>) allMetrics.get("persistence");
        if (persistenceMetrics != null && !persistenceMetrics.isEmpty()) {
            result.append("# Persistence\r\n");
            persistenceMetrics.forEach((key, value) -> result.append(key).append(":").append(value).append("\r\n"));
            result.append("\r\n");
        }

        return result.toString();
    }

    /**
     * Get metrics for a specific section.
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
                sectionMetrics.put("active_connections", allMetrics.get("active_connections"));
                sectionMetrics.put("total_commands_processed", allMetrics.get("total_commands_processed"));
                sectionMetrics.put("total_errors", allMetrics.get("total_errors"));
                sectionMetrics.put("memory_usage_bytes", allMetrics.get("memory_usage_bytes"));
                sectionMetrics.put("uptime_seconds", allMetrics.get("uptime_seconds"));
                break;
            case "commands":
                @SuppressWarnings("unchecked")
                Map<String, Object> commandMetrics = (Map<String, Object>) allMetrics.get("commands");
                if (commandMetrics != null) {
                    sectionMetrics.putAll(commandMetrics);
                }
                break;
            case "storage":
                @SuppressWarnings("unchecked")
                Map<String, Object> storageMetrics = (Map<String, Object>) allMetrics.get("storage");
                if (storageMetrics != null) {
                    sectionMetrics.putAll(storageMetrics);
                }
                break;
            case "replication":
                @SuppressWarnings("unchecked")
                Map<String, Object> replicationMetrics = (Map<String, Object>) allMetrics.get("replication");
                if (replicationMetrics != null) {
                    sectionMetrics.putAll(replicationMetrics);
                }
                break;
            case "pubsub":
                @SuppressWarnings("unchecked")
                Map<String, Object> pubsubMetrics = (Map<String, Object>) allMetrics.get("pubsub");
                if (pubsubMetrics != null) {
                    sectionMetrics.putAll(pubsubMetrics);
                }
                break;
            case "transactions":
                @SuppressWarnings("unchecked")
                Map<String, Object> transactionMetrics = (Map<String, Object>) allMetrics.get("transactions");
                if (transactionMetrics != null) {
                    sectionMetrics.putAll(transactionMetrics);
                }
                break;
            case "persistence":
                @SuppressWarnings("unchecked")
                Map<String, Object> persistenceMetrics = (Map<String, Object>) allMetrics.get("persistence");
                if (persistenceMetrics != null) {
                    sectionMetrics.putAll(persistenceMetrics);
                }
                break;
            default:
                log.warn("Unknown metrics section requested: {}", section);
                break;
        }

        return sectionMetrics;
    }

    /**
     * Get all metrics as a map.
     * 
     * @return Map containing all metrics
     */
    public Map<String, Object> getAllMetrics() {
        return metricsCollector.getAllMetrics();
    }

    /**
     * Get metrics in Prometheus format.
     * 
     * @return Prometheus-formatted metrics string
     */
    public String getMetricsAsPrometheus() {
        Map<String, Object> allMetrics = metricsCollector.getAllMetrics();
        StringBuilder result = new StringBuilder();

        // Add basic server metrics
        appendPrometheusMetric(result, "redis_connected_clients", allMetrics.get("active_connections"),
                "Number of client connections");
        appendPrometheusMetric(result, "redis_commands_processed_total", allMetrics.get("total_commands_processed"),
                "Total number of commands processed");
        appendPrometheusMetric(result, "redis_errors_total", allMetrics.get("total_errors"), "Total number of errors");
        appendPrometheusMetric(result, "redis_memory_usage_bytes", allMetrics.get("memory_usage_bytes"),
                "Memory usage in bytes");
        appendPrometheusMetric(result, "redis_uptime_seconds", allMetrics.get("uptime_seconds"),
                "Server uptime in seconds");

        // Add command metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> commandMetrics = (Map<String, Object>) allMetrics.get("commands");
        if (commandMetrics != null) {
            commandMetrics.forEach((key, value) -> {
                if (key.endsWith("_count")) {
                    String commandName = key.substring(0, key.length() - 6);
                    appendPrometheusMetricWithTags(result, "redis_command_executions_total", value,
                            "Number of executions for command", "command", commandName);
                } else if (key.endsWith("_errors")) {
                    String commandName = key.substring(0, key.length() - 7);
                    appendPrometheusMetricWithTags(result, "redis_command_errors_total", value,
                            "Number of errors for command", "command", commandName);
                }
            });
        }

        // Add other metrics sections...

        return result.toString();
    }

    private void appendPrometheusMetric(StringBuilder sb, String name, Object value, String help) {
        sb.append("# HELP ").append(name).append(" ").append(help).append("\n");
        sb.append("# TYPE ").append(name).append(" gauge\n");
        sb.append(name).append(" ").append(value).append("\n");
    }

    private void appendPrometheusMetricWithTags(StringBuilder sb, String name, Object value, String help,
            String... tags) {
        sb.append("# HELP ").append(name).append(" ").append(help).append("\n");
        sb.append("# TYPE ").append(name).append(" counter\n");
        sb.append(name).append("{");
        for (int i = 0; i < tags.length; i += 2) {
            if (i > 0)
                sb.append(",");
            sb.append(tags[i]).append("=\"").append(tags[i + 1]).append("\"");
        }
        sb.append("} ").append(value).append("\n");
    }
}
