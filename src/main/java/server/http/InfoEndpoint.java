package server.http;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

import server.ServerContext;

/**
 * Example custom HTTP endpoint demonstrating extensibility.
 * This endpoint returns basic server information.
 */
public final class InfoEndpoint implements HttpEndpoint {

    private final ServerContext serverContext;

    public InfoEndpoint(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public String getPath() {
        return "/info";
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
            String serverInfo = generateServerInfo();
            sendResponse(exchange, 200, serverInfo);
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private String generateServerInfo() {
        var config = serverContext.getConfig();
        var replicationState = serverContext.getReplicationState();

        return String.format("""
                {
                  "server": {
                    "version": "1.0",
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
                config.port(),
                config.bindAddress(),
                replicationState.getRole().toString().toLowerCase(),
                config.appendOnlyMode(),
                config.httpServerEnabled(),
                config.httpPort(),
                replicationState.getRole().toString().toLowerCase(),
                config.isReplicaMode() ? config.getMasterInfo().host() : "none",
                config.isReplicaMode() ? config.getMasterInfo().port() : 0,
                config.maxMemory());
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
