package server.http;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import server.ServerContext;

/**
 * HTTP endpoint that provides a simple health check for the server.
 * Returns server status, timestamp, uptime, version, and port in JSON format.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class HealthEndpoint implements HttpEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthEndpoint.class);

    private static final String HEALTH_PATH = "/health";
    private static final String[] SUPPORTED_METHODS = { "GET" };
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_TEXT = "text/plain";
    private static final String VERSION = "1.0";
    private static final String UPTIME_UNKNOWN = "unknown";

    private final ServerContext serverContext;

    public HealthEndpoint(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public String getPath() {
        return HEALTH_PATH;
    }

    @Override
    public String[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE_JSON;
    }

    /**
     * Handles HTTP requests for the health endpoint.
     * Responds with server health status in JSON for GET requests.
     *
     * @param exchange the HTTP exchange object
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        if (!"GET".equals(requestMethod)) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            String healthStatusJson = buildHealthStatusJson();
            sendJsonResponse(exchange, 200, healthStatusJson);
            LOGGER.debug("Health check responded with status UP");
        } catch (Exception exception) {
            LOGGER.error("Health check failed", exception);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + exception.getMessage());
        }
    }

    /**
     * Builds the JSON response for the health status.
     *
     * @return JSON string representing the health status
     */
    private String buildHealthStatusJson() {
        boolean isHealthy = serverContext != null;
        int redisPort = serverContext != null ? serverContext.getConfig().port() : -1;

        return String.format("""
                {
                  "status": "%s",
                  "timestamp": %d,
                  "uptime": "%s",
                  "version": "%s",
                  "redis_port": %d
                }
                """,
                isHealthy ? "UP" : "DOWN",
                System.currentTimeMillis(),
                UPTIME_UNKNOWN,
                VERSION,
                redisPort);
    }

    /**
     * Sends a JSON response with the given status code and body.
     *
     * @param exchange     the HTTP exchange object
     * @param statusCode   the HTTP status code
     * @param responseBody the response body as a JSON string
     * @throws IOException if an I/O error occurs
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        sendResponse(exchange, statusCode, responseBody, CONTENT_TYPE_JSON);
    }

    /**
     * Sends an error response with the given status code and message.
     *
     * @param exchange   the HTTP exchange object
     * @param statusCode the HTTP status code
     * @param message    the error message
     * @throws IOException if an I/O error occurs
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendResponse(exchange, statusCode, message, CONTENT_TYPE_TEXT);
    }

    /**
     * Sends a response with the given status code, body, and content type.
     *
     * @param exchange     the HTTP exchange object
     * @param statusCode   the HTTP status code
     * @param responseBody the response body
     * @param contentType  the content type
     * @throws IOException if an I/O error occurs
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody, String contentType)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] responseBytes = responseBody.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}
