package server.http;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import metrics.MetricsCollector;
import server.ServerContext;

/**
 * HTTP endpoint that exposes server metrics in Prometheus-compatible plain text
 * format.
 * Used for monitoring and observability.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class MetricsEndpoint implements HttpEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsEndpoint.class);

    // Endpoint path
    private static final String METRICS_PATH = "/metrics";
    // Supported HTTP methods
    private static final String[] SUPPORTED_METHODS = { "GET" };
    // Content type for Prometheus metrics
    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";
    // Error messages
    private static final String ERROR_METHOD_NOT_ALLOWED = "Method Not Allowed";
    private static final String ERROR_INTERNAL_SERVER = "Internal Server Error: ";
    private static final String METRICS_NOT_AVAILABLE = "# Metrics not available\n";

    private final ServerContext serverContext;

    /**
     * Constructs a MetricsEndpoint with the given server context.
     * 
     * @param serverContext the server context providing metrics
     */
    public MetricsEndpoint(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public String getPath() {
        return METRICS_PATH;
    }

    @Override
    public String[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    /**
     * Handles HTTP requests to the /metrics endpoint.
     * Only GET requests are supported.
     *
     * @param exchange the HTTP exchange object
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        if (!"GET".equals(requestMethod)) {
            sendErrorResponse(exchange, 405, ERROR_METHOD_NOT_ALLOWED);
            return;
        }

        try {
            String metricsOutput = generateMetrics();
            sendResponse(exchange, 200, metricsOutput);
            LOGGER.debug("Served metrics successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to generate metrics", e);
            sendErrorResponse(exchange, 500, ERROR_INTERNAL_SERVER + e.getMessage());
        }
    }

    /**
     * Generates the metrics output in Prometheus format.
     * 
     * @return metrics string
     */
    private String generateMetrics() {
        var metricsHandler = serverContext.getMetricsHandler();
        if (metricsHandler == null) {
            LOGGER.warn("Metrics handler not available.");
            return METRICS_NOT_AVAILABLE;
        }

        String infoMetrics = metricsHandler.getMetricsAsInfo();
        return convertToPrometheusFormat(infoMetrics);
    }

    /**
     * Converts metrics from INFO format to Prometheus format.
     * 
     * @param infoMetrics metrics in INFO format
     * @return metrics in Prometheus format
     */
    private String convertToPrometheusFormat(String infoMetrics) {
        StringBuilder prometheusBuilder = new StringBuilder();
        prometheusBuilder.append("# Redis-like server metrics (Redis Enterprise Compatible)\n");
        prometheusBuilder.append("# HELP redis_info_metric Redis server metrics\n");
        prometheusBuilder.append("# TYPE redis_info_metric gauge\n");

        var collector = serverContext.getMetricsHandler().getMetricsCollector();

        appendEndpointMetrics(prometheusBuilder, collector);
        appendKeyspaceMetrics(prometheusBuilder, collector);
        appendKeyTypeAndNetworkMetrics(prometheusBuilder, collector);
        appendLegacyMetrics(prometheusBuilder, infoMetrics);

        return prometheusBuilder.toString();
    }

    /**
     * Appends endpoint-related metrics to the builder.
     */
    private void appendEndpointMetrics(StringBuilder builder, MetricsCollector collector) {
        builder.append("\n# HELP endpoint_client_connections Number of client connection establishment events\n")
                .append("# TYPE endpoint_client_connections counter\n")
                .append("endpoint_client_connections ").append(collector.getClientConnections()).append("\n")
                .append("# HELP endpoint_client_disconnections Number of client disconnections\n")
                .append("# TYPE endpoint_client_disconnections counter\n")
                .append("endpoint_client_disconnections ").append(collector.getClientDisconnections()).append("\n")
                .append("# HELP endpoint_read_requests Number of read requests\n")
                .append("# TYPE endpoint_read_requests counter\n")
                .append("endpoint_read_requests ").append(collector.getReadRequests()).append("\n")
                .append("# HELP endpoint_write_requests Number of write requests\n")
                .append("# TYPE endpoint_write_requests counter\n")
                .append("endpoint_write_requests ").append(collector.getWriteRequests()).append("\n")
                .append("# HELP endpoint_other_requests Number of other requests\n")
                .append("# TYPE endpoint_other_requests counter\n")
                .append("endpoint_other_requests ").append(collector.getOtherRequests()).append("\n");
    }

    /**
     * Appends keyspace-related metrics to the builder.
     */
    private void appendKeyspaceMetrics(StringBuilder builder, MetricsCollector collector) {
        builder.append("\n# HELP redis_keyspace_read_hits Number of read operations accessing existing keyspace\n")
                .append("# TYPE redis_keyspace_read_hits counter\n")
                .append("redis_keyspace_read_hits ").append(collector.getKeyspaceReadHits()).append("\n")
                .append("# HELP redis_keyspace_read_misses Number of read operations accessing non-existing keyspace\n")
                .append("# TYPE redis_keyspace_read_misses counter\n")
                .append("redis_keyspace_read_misses ").append(collector.getKeyspaceReadMisses()).append("\n")
                .append("# HELP redis_keyspace_write_hits Number of write operations accessing existing keyspace\n")
                .append("# TYPE redis_keyspace_write_hits counter\n")
                .append("redis_keyspace_write_hits ").append(collector.getKeyspaceWriteHits()).append("\n")
                .append("# HELP redis_keyspace_write_misses Number of write operations accessing non-existing keyspace\n")
                .append("# TYPE redis_keyspace_write_misses counter\n")
                .append("redis_keyspace_write_misses ").append(collector.getKeyspaceWriteMisses()).append("\n");
    }

    /**
     * Appends key type distribution and network metrics to the builder.
     */
    private void appendKeyTypeAndNetworkMetrics(StringBuilder builder, MetricsCollector collector) {
        builder.append("\n# HELP redis_server_total_keys Total number of keys\n")
                .append("# TYPE redis_server_total_keys gauge\n")
                .append("redis_server_total_keys ").append(collector.getTotalKeys()).append("\n")
                .append("# HELP redis_server_blocked_clients Count of clients waiting on blocking calls\n")
                .append("# TYPE redis_server_blocked_clients gauge\n")
                .append("redis_server_blocked_clients ").append(collector.getBlockedClients()).append("\n")
                .append("# HELP redis_server_master_repl_offset Master replication offset\n")
                .append("# TYPE redis_server_master_repl_offset gauge\n")
                .append("redis_server_master_repl_offset ").append(collector.getMasterReplOffset()).append("\n")
                .append("\n# HELP redis_server_total_net_input_bytes Total bytes received\n")
                .append("# TYPE redis_server_total_net_input_bytes counter\n")
                .append("redis_server_total_net_input_bytes ").append(collector.getTotalNetInputBytes()).append("\n")
                .append("# HELP redis_server_total_net_output_bytes Total bytes sent\n")
                .append("# TYPE redis_server_total_net_output_bytes counter\n")
                .append("redis_server_total_net_output_bytes ").append(collector.getTotalNetOutputBytes()).append("\n")
                .append("# HELP redis_server_total_connections_received Total connections received\n")
                .append("# TYPE redis_server_total_connections_received counter\n")
                .append("redis_server_total_connections_received ").append(collector.getTotalConnectionsReceived())
                .append("\n");
    }

    /**
     * Appends legacy INFO-format metrics to the builder.
     */
    private void appendLegacyMetrics(StringBuilder builder, String infoMetrics) {
        builder.append("\n# Legacy metrics from INFO format\n");
        String[] lines = infoMetrics.split("\\r?\\n");
        String currentSection = "";

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("#")) {
                currentSection = trimmedLine.substring(1).trim().toLowerCase();
                builder.append("\n# ").append(currentSection.toUpperCase()).append(" section\n");
            } else if (trimmedLine.contains(":") && !trimmedLine.isEmpty()) {
                String[] parts = trimmedLine.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    try {
                        double numericValue = Double.parseDouble(value);
                        builder.append("redis_").append(currentSection).append("_").append(key)
                                .append(" ").append(numericValue).append("\n");
                    } catch (NumberFormatException e) {
                        builder.append("redis_").append(currentSection).append("_").append(key)
                                .append("{value=\"").append(value).append("\"} 1\n");
                    }
                }
            }
        }
    }

    /**
     * Sends a successful HTTP response with the given status and body.
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", getContentType());
        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Sends an error HTTP response with the given status and message.
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        byte[] messageBytes = message.getBytes();
        exchange.sendResponseHeaders(statusCode, messageBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(messageBytes);
        }
    }
}
