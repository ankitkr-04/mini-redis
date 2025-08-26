package server.http;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import server.ServerContext;

/**
 * HTTP endpoint that returns basic server and replication information in JSON
 * format.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class InfoEndpoint implements HttpEndpoint {

    // Endpoint path
    private static final String ENDPOINT_PATH = "/info";
    // Supported HTTP methods
    private static final String[] SUPPORTED_METHODS = { "GET" };
    // Content type for JSON responses
    private static final String CONTENT_TYPE_JSON = "application/json";
    // Content type for plain text error responses
    private static final String CONTENT_TYPE_TEXT = "text/plain";
    // Server version
    private static final String SERVER_VERSION = "1.0";

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoEndpoint.class);

    private final ServerContext serverContext;

    /**
     * Constructs an InfoEndpoint with the given server context.
     * 
     * @param serverContext the server context containing configuration and state
     */
    public InfoEndpoint(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public String getPath() {
        return ENDPOINT_PATH;
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
     * Handles HTTP requests to the /info endpoint.
     * Responds with server and replication information in JSON format for GET
     * requests.
     * Returns 405 for unsupported methods and 500 for internal errors.
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
            String serverInfoJson = buildServerInfoJson();
            sendJsonResponse(exchange, 200, serverInfoJson);
            LOGGER.debug("Responded to /info request with server info JSON.");
        } catch (Exception ex) {
            LOGGER.error("Failed to generate server info JSON", ex);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + ex.getMessage());
        }
    }

    /**
     * Builds the JSON string containing server and replication information.
     *
     * @return JSON string with server info
     */
    private String buildServerInfoJson() {
        var config = serverContext.getConfig();
        var replicationState = serverContext.getReplicationState();

        String role = replicationState.getRole().toString().toLowerCase();
        String masterHost = config.isReplicaMode() ? config.getMasterInfo().host() : "none";
        int masterPort = config.isReplicaMode() ? config.getMasterInfo().port() : 0;

        return String.format("""
                {
                  "server": {
                    "version": "%s",
                    "port": %d,
                    "bind_address": "%s",
                    "role": "%s",
                    "aof_enabled": %s,
                    "http_enabled": %s,
                    "http_port": %d
                  },
                  "replication": {
                    "role": "%s",
                    "connected_replicas": 0,
                    "master_host": "%s",
                    "master_port": %d
                  },
                  "memory": {
                    "max_memory": %d,
                    "used_memory": "unknown"
                  }
                }
                """,
                SERVER_VERSION,
                config.port(),
                config.bindAddress(),
                role,
                config.appendOnlyMode(),
                config.httpServerEnabled(),
                config.httpPort(),
                role,
                masterHost,
                masterPort,
                config.maxMemory());
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
     * Sends an HTTP response with the specified status code, body, and content
     * type.
     *
     * @param exchange     the HTTP exchange object
     * @param statusCode   the HTTP status code
     * @param responseBody the response body
     * @param contentType  the content type header value
     * @throws IOException if an I/O error occurs
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody, String contentType)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] responseBytes = responseBody.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
