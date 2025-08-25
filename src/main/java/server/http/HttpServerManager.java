package server.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import server.ServerContext;

/**
 * Manages the HTTP server for exposing metrics and other endpoints.
 * Provides an extensible framework for registering HTTP endpoints.
 */
public final class HttpServerManager {
    private static final Logger log = LoggerFactory.getLogger(HttpServerManager.class);

    private final ServerContext serverContext;
    private final int port;
    private final String bindAddress;
    private final List<HttpEndpoint> endpoints;
    private HttpServer httpServer;

    public HttpServerManager(ServerContext serverContext, String bindAddress, int port) {
        this.serverContext = serverContext;
        this.bindAddress = bindAddress;
        this.port = port;
        this.endpoints = new ArrayList<>();

        // Register default endpoints
        registerDefaultEndpoints();
    }

    /**
     * Register a new HTTP endpoint.
     * 
     * @param endpoint the endpoint to register
     */
    public void registerEndpoint(HttpEndpoint endpoint) {
        endpoints.add(endpoint);
        if (httpServer != null) {
            // Server is already running, register the endpoint immediately
            registerEndpointWithServer(endpoint);
        }
    }

    /**
     * Start the HTTP server.
     * 
     * @throws IOException if the server cannot be started
     */
    public void start() throws IOException {
        if (httpServer != null) {
            log.warn("HTTP server is already running");
            return;
        }

        httpServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);

        // Register all endpoints
        for (HttpEndpoint endpoint : endpoints) {
            registerEndpointWithServer(endpoint);
        }

        // Use a thread pool executor
        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.start();

        log.info("HTTP server started on {}:{}", bindAddress, port);
    }

    /**
     * Stop the HTTP server.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            log.info("HTTP server stopped");
        }
    }

    private void registerDefaultEndpoints() {
        // Register metrics endpoint
        registerEndpoint(new MetricsEndpoint(serverContext));

        // Register health check endpoint
        registerEndpoint(new HealthEndpoint(serverContext));

        // Register info endpoint
        registerEndpoint(new InfoEndpoint(serverContext));
    }

    private void registerEndpointWithServer(HttpEndpoint endpoint) {
        httpServer.createContext(endpoint.getPath(), endpoint::handle);
        log.debug("Registered HTTP endpoint: {} (methods: {})",
                endpoint.getPath(), String.join(", ", endpoint.getSupportedMethods()));
    }
}
