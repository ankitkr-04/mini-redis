package server.http;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

/**
 * Interface for HTTP endpoints that can be registered with the HTTP server.
 */
public interface HttpEndpoint {

    /**
     * Get the path pattern this endpoint handles.
     * 
     * @return the path pattern (e.g., "/metrics", "/health")
     */
    String getPath();

    /**
     * Get the HTTP methods this endpoint supports.
     * 
     * @return array of supported HTTP methods (e.g., "GET", "POST")
     */
    String[] getSupportedMethods();

    /**
     * Handle the HTTP request.
     * 
     * @param exchange the HTTP exchange
     * @throws IOException if an I/O error occurs
     */
    void handle(HttpExchange exchange) throws IOException;

    /**
     * Get the content type this endpoint returns.
     * 
     * @return the content type (e.g., "text/plain", "application/json")
     */
    default String getContentType() {
        return "text/plain";
    }
}
