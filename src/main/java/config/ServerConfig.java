package config;

public final class ServerConfig {
    private ServerConfig() {
    } // Utility class

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

    // Configuration parsing constants
    public static final String OPTION_PREFIX = "--";
    public static final int OPTION_PREFIX_LENGTH = 2;

    // Port validation
    public static final int MIN_PORT_VALUE = 1;
    public static final int MAX_PORT_VALUE = 65535;

    // HTTP configuration
    public static final int DEFAULT_HTTP_PORT = 8080;
    public static final boolean DEFAULT_HTTP_ENABLED = false;
    public static final boolean DEFAULT_APPEND_ONLY = false;

    // Configuration limits
    public static final int MASTER_INFO_PARTS_COUNT = 2;
    public static final String MASTER_INFO_DELIMITER = "\\s+";

    // Metrics Configuration
    public static final String METRICS_REGISTRY_NAME = "redis-server";
    public static final int METRICS_COLLECTION_INTERVAL_MS = 1000;
    public static final int METRICS_HISTORY_SIZE = 300; // 5 minutes at 1-second intervals

    // Memory size thresholds for Redis Enterprise compatibility
    public static final long SIZE_128M = 128L * 1024L * 1024L;
    public static final long SIZE_512M = 512L * 1024L * 1024L;
    public static final long SIZE_1M = 1024L * 1024L;
    public static final long SIZE_8M = 8L * 1024L * 1024L;
}
