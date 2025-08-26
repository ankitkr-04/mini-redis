package server.http;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

import server.ServerContext;

/**
 * HTTP endpoint that exposes server metrics in plain text format.
 * This endpoint is compatible with monitoring systems like Prometheus.
 */
public final class MetricsEndpoint implements HttpEndpoint {

    private final ServerContext serverContext;

    public MetricsEndpoint(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public String getPath() {
        return "/metrics";
    }

    @Override
    public String[] getSupportedMethods() {
        return new String[] { "GET" };
    }

    @Override
    public String getContentType() {
        return "text/plain; charset=utf-8";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (!"GET".equals(method)) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            String metricsOutput = generateMetrics();
            sendResponse(exchange, 200, metricsOutput);
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private String generateMetrics() {
        var metricsHandler = serverContext.getMetricsHandler();
        if (metricsHandler == null) {
            return "# Metrics not available\n";
        }

        // Get all metrics in INFO format and convert to Prometheus-like format
        String infoMetrics = metricsHandler.getMetricsAsInfo();
        return convertToPrometheusFormat(infoMetrics);
    }

    private String convertToPrometheusFormat(String infoMetrics) {
        StringBuilder prometheus = new StringBuilder();
        prometheus.append("# Redis-like server metrics (Redis Enterprise Compatible)\n");
        prometheus.append("# HELP redis_info_metric Redis server metrics\n");
        prometheus.append("# TYPE redis_info_metric gauge\n");

        var collector = serverContext.getMetricsHandler().getMetricsCollector();

        // Add Redis Enterprise compatible endpoint metrics
        prometheus.append("\n# HELP endpoint_client_connections Number of client connection establishment events\n");
        prometheus.append("# TYPE endpoint_client_connections counter\n");
        prometheus.append("endpoint_client_connections ").append(collector.getClientConnections()).append("\n");

        prometheus.append("# HELP endpoint_client_disconnections Number of client disconnections\n");
        prometheus.append("# TYPE endpoint_client_disconnections counter\n");
        prometheus.append("endpoint_client_disconnections ").append(collector.getClientDisconnections()).append("\n");

        prometheus.append("# HELP endpoint_read_requests Number of read requests\n");
        prometheus.append("# TYPE endpoint_read_requests counter\n");
        prometheus.append("endpoint_read_requests ").append(collector.getReadRequests()).append("\n");

        prometheus.append("# HELP endpoint_write_requests Number of write requests\n");
        prometheus.append("# TYPE endpoint_write_requests counter\n");
        prometheus.append("endpoint_write_requests ").append(collector.getWriteRequests()).append("\n");

        prometheus.append("# HELP endpoint_other_requests Number of other requests\n");
        prometheus.append("# TYPE endpoint_other_requests counter\n");
        prometheus.append("endpoint_other_requests ").append(collector.getOtherRequests()).append("\n");

        // Add keyspace metrics
        prometheus.append("\n# HELP redis_keyspace_read_hits Number of read operations accessing existing keyspace\n");
        prometheus.append("# TYPE redis_keyspace_read_hits counter\n");
        prometheus.append("redis_keyspace_read_hits ").append(collector.getKeyspaceReadHits()).append("\n");

        prometheus.append(
                "# HELP redis_keyspace_read_misses Number of read operations accessing non-existing keyspace\n");
        prometheus.append("# TYPE redis_keyspace_read_misses counter\n");
        prometheus.append("redis_keyspace_read_misses ").append(collector.getKeyspaceReadMisses()).append("\n");

        prometheus.append("# HELP redis_keyspace_write_hits Number of write operations accessing existing keyspace\n");
        prometheus.append("# TYPE redis_keyspace_write_hits counter\n");
        prometheus.append("redis_keyspace_write_hits ").append(collector.getKeyspaceWriteHits()).append("\n");

        prometheus.append(
                "# HELP redis_keyspace_write_misses Number of write operations accessing non-existing keyspace\n");
        prometheus.append("# TYPE redis_keyspace_write_misses counter\n");
        prometheus.append("redis_keyspace_write_misses ").append(collector.getKeyspaceWriteMisses()).append("\n");

        // Add key type distribution metrics
        prometheus.append("\n# HELP redis_server_total_keys Total number of keys\n");
        prometheus.append("# TYPE redis_server_total_keys gauge\n");
        prometheus.append("redis_server_total_keys ").append(collector.getTotalKeys()).append("\n");

        prometheus.append("# HELP redis_server_blocked_clients Count of clients waiting on blocking calls\n");
        prometheus.append("# TYPE redis_server_blocked_clients gauge\n");
        prometheus.append("redis_server_blocked_clients ").append(collector.getBlockedClients()).append("\n");

        prometheus.append("# HELP redis_server_master_repl_offset Master replication offset\n");
        prometheus.append("# TYPE redis_server_master_repl_offset gauge\n");
        prometheus.append("redis_server_master_repl_offset ").append(collector.getMasterReplOffset()).append("\n");

        // Add network metrics
        prometheus.append("\n# HELP redis_server_total_net_input_bytes Total bytes received\n");
        prometheus.append("# TYPE redis_server_total_net_input_bytes counter\n");
        prometheus.append("redis_server_total_net_input_bytes ").append(collector.getTotalNetInputBytes()).append("\n");

        prometheus.append("# HELP redis_server_total_net_output_bytes Total bytes sent\n");
        prometheus.append("# TYPE redis_server_total_net_output_bytes counter\n");
        prometheus.append("redis_server_total_net_output_bytes ").append(collector.getTotalNetOutputBytes())
                .append("\n");

        prometheus.append("# HELP redis_server_total_connections_received Total connections received\n");
        prometheus.append("# TYPE redis_server_total_connections_received counter\n");
        prometheus.append("redis_server_total_connections_received ").append(collector.getTotalConnectionsReceived())
                .append("\n");

        // Legacy metrics from INFO format
        prometheus.append("\n# Legacy metrics from INFO format\n");
        String[] lines = infoMetrics.split("\\r?\\n");
        String currentSection = "";

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#")) {
                // Section header
                currentSection = line.substring(1).trim().toLowerCase();
                prometheus.append("\n# ").append(currentSection.toUpperCase()).append(" section\n");
            } else if (line.contains(":") && !line.isEmpty()) {
                // Metric line
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    // Try to parse as number for Prometheus
                    try {
                        double numValue = Double.parseDouble(value);
                        prometheus.append("redis_").append(currentSection).append("_").append(key)
                                .append(" ").append(numValue).append("\n");
                    } catch (NumberFormatException e) {
                        // Non-numeric values as info labels
                        prometheus.append("redis_").append(currentSection).append("_").append(key)
                                .append("{value=\"").append(value).append("\"} 1\n");
                    }
                }
            }
        }

        return prometheus.toString();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", getContentType());
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(statusCode, message.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }
}
