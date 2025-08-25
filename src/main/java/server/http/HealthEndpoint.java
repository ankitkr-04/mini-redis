package server.http;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

import server.ServerContext;

/**
 * HTTP endpoint that provides a simple health check for the server.
 */
public final class HealthEndpoint implements HttpEndpoint {

    private final ServerContext serverContext;

    public HealthEndpoint(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public String getPath() {
        return "/health";
    }

    @Override
    public String[] getSupportedMethods() {
        return new String[] { "GET" };
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (!"GET".equals(method)) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            String healthStatus = generateHealthStatus();
            sendResponse(exchange, 200, healthStatus);
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private String generateHealthStatus() {
        // Simple health check - server is healthy if context is running
        boolean isHealthy = serverContext != null;

        return String.format("""
                {
                  "status": "%s",
                  "timestamp": %d,
                  "uptime": "unknown",
                  "version": "1.0",
                  "redis_port": %d
                }
                """,
                isHealthy ? "UP" : "DOWN",
                System.currentTimeMillis(),
                serverContext.getConfig().port());
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
