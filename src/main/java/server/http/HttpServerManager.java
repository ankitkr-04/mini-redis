package server.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import server.ServerContext;

/**
 * Manages the HTTP server for exposing metrics and other endpoints.
 * Allows registration of custom HTTP endpoints.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class HttpServerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerManager.class);

    private static final int DEFAULT_THREAD_POOL_SIZE = 4;

    private final ServerContext serverContext;
    private final int serverPort;
    private final String serverBindAddress;
    private final List<HttpEndpoint> registeredEndpoints;
    private HttpServer httpServer;
    private ExecutorService executorService;

    /**
     * Constructs an HttpServerManager with the given context, bind address, and
     * port.
     * Registers default endpoints for metrics, health, and info.
     *
     * @param serverContext     the server context
     * @param serverBindAddress the address to bind the HTTP server
     * @param serverPort        the port to bind the HTTP server
     */
    public HttpServerManager(ServerContext serverContext, String serverBindAddress, int serverPort) {
        this.serverContext = serverContext;
        this.serverBindAddress = serverBindAddress;
        this.serverPort = serverPort;
        this.registeredEndpoints = new ArrayList<>();
        registerDefaultEndpoints();
    }

    /**
     * Registers a new HTTP endpoint. If the server is running, registers it
     * immediately.
     *
     * @param endpoint the endpoint to register
     */
    public void registerEndpoint(HttpEndpoint endpoint) {
        registeredEndpoints.add(endpoint);
        if (httpServer != null) {
            addEndpointToServer(endpoint);
        }
    }

    /**
     * Starts the HTTP server and registers all endpoints.
     *
     * @throws IOException if the server cannot be started
     */
    public void start() throws IOException {
        if (httpServer != null) {
            LOGGER.info("HTTP server is already running");
            return;
        }

        httpServer = HttpServer.create(new InetSocketAddress(serverBindAddress, serverPort), 0);

        for (HttpEndpoint endpoint : registeredEndpoints) {
            addEndpointToServer(endpoint);
        }

        executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
        httpServer.setExecutor(executorService);
        httpServer.start();

        LOGGER.info("HTTP server started on {}:{}", serverBindAddress, serverPort);
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            if (executorService != null) {
                executorService.shutdownNow();
                executorService = null;
            }
            LOGGER.info("HTTP server stopped");
        }
    }

    /**
     * Registers the default endpoints: metrics, health, and info.
     */
    private void registerDefaultEndpoints() {
        registerEndpoint(new MetricsEndpoint(serverContext));
        registerEndpoint(new HealthEndpoint(serverContext));
        registerEndpoint(new InfoEndpoint(serverContext));
    }

    /**
     * Adds an endpoint to the running HTTP server.
     *
     * @param endpoint the endpoint to add
     */
    private void addEndpointToServer(HttpEndpoint endpoint) {
        httpServer.createContext(endpoint.getPath(), endpoint::handle);
        LOGGER.debug("Registered HTTP endpoint: {} (methods: {})",
                endpoint.getPath(), String.join(", ", endpoint.getSupportedMethods()));
    }
}
