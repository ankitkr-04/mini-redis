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
        prometheus.append("# Redis-like server metrics\n");
        prometheus.append("# TYPE redis_info_metric gauge\n");

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
