package config;

public final class ServerConfig {
    private ServerConfig() {} // Utility class

    // Server Configuration
    public static final int DEFAULT_PORT = 6379;
    public static final int BUFFER_SIZE = 1024;
    public static final int CLEANUP_INTERVAL_MS = 100;

    // Threading Configuration
    public static final int IO_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());

    // Connection Configuration
    public static final int MAX_CONNECTIONS = 10000;
    public static final int SOCKET_TIMEOUT_MS = 30000;

    // Memory Configuration
    public static final long MAX_MEMORY_BYTES = 100 * 1024 * 1024; // 100MB default
    public static final double MEMORY_CLEANUP_THRESHOLD = 0.9;

    // Replication Configuration
    public static final long DEFAULT_REPL_BACKLOG_SIZE = 1 * 1024 * 1024; // 1MB

    // Additional Configuration
    public static final String DEFAULT_DIR = "/var/lib/redis";
    public static final String DEFAULT_DB_FILENAME = "dump.rdb";
    public static final long DEFAULT_MAX_MEMORY = MAX_MEMORY_BYTES;
    public static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";
}
