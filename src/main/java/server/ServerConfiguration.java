package server;

import java.util.Map;
import java.util.Optional;

import config.ConfigurationParser;
import config.ConfigurationParser.MasterInfo;
import config.ServerConfig;

/**
 * Immutable configuration record for the Redis server.
 * 
 * <p>
 * This record holds all configuration parameters needed to start and run
 * the Redis server instance. It provides static factory methods to create
 * configurations from command-line options and includes helper methods
 * for common configuration queries.
 * </p>
 * 
 * @param port                   the port number the server listens on
 * @param masterInfo             optional master information for replica mode
 * @param replicationBacklogSize size of the replication backlog in bytes
 * @param dataDirectory          directory for persistent data storage
 * @param databaseFilename       filename for the RDB database file
 * @param appendOnlyMode         whether AOF (Append Only File) mode is enabled
 * @param maxMemory              maximum memory usage in bytes
 * @param bindAddress            IP address to bind the server to
 * @param requirePassword        optional password for client authentication
 * @param httpServerEnabled      whether the HTTP management interface is
 *                               enabled
 * @param httpPort               port number for the HTTP management interface
 * 
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public record ServerConfiguration(
        int port,
        Optional<MasterInfo> masterInfo,
        long replicationBacklogSize,
        String dataDirectory,
        String databaseFilename,
        boolean appendOnlyMode,
        long maxMemory,
        String bindAddress,
        Optional<String> requirePassword,
        boolean httpServerEnabled,
        int httpPort) {

    /** Configuration parameter name for port */
    private static final String PARAM_PORT = "port";

    /** Configuration parameter name for replication backlog size */
    private static final String PARAM_REPL_BACKLOG_SIZE = "repl-backlog-size";

    /** Configuration parameter name for data directory */
    private static final String PARAM_DIR = "dir";

    /** Configuration parameter name for database filename */
    private static final String PARAM_DBFILENAME = "dbfilename";

    /** Configuration parameter name for append only mode */
    private static final String PARAM_APPENDONLY = "appendonly";

    /** Configuration parameter name for maximum memory */
    private static final String PARAM_MAXMEMORY = "maxmemory";

    /** Configuration parameter name for bind address */
    private static final String PARAM_BIND = "bind";

    /** Configuration parameter name for required password */
    private static final String PARAM_REQUIREPASS = "requirepass";

    /** Configuration parameter name for replica master host */
    private static final String PARAM_MASTER_HOST = "master_host";

    /** Configuration parameter name for replica master port */
    private static final String PARAM_MASTER_PORT = "master_port";

    /** Configuration parameter name for replica of */
    private static final String PARAM_REPLICAOF = "replicaof";

    /** Configuration parameter name for HTTP enabled */
    private static final String PARAM_HTTP_ENABLED = "http-enabled";

    /** Configuration parameter name for HTTP port */
    private static final String PARAM_HTTP_PORT = "http-port";

    /** Configuration value for boolean true */
    private static final String BOOLEAN_YES = "yes";

    /** Configuration value for boolean false */
    private static final String BOOLEAN_NO = "no";

    /** Default port value when not in replica mode */
    private static final String DEFAULT_MASTER_PORT_STRING = "0";

    /** Empty string for missing configuration values */
    private static final String EMPTY_STRING = "";

    /**
     * Creates a ServerConfiguration from a map of configuration options.
     * 
     * @param options map of configuration key-value pairs
     * @return a new ServerConfiguration instance with the specified options
     */
    public static ServerConfiguration from(Map<String, String> options) {
        return new ServerConfiguration(
                ConfigurationParser.getIntOption(options, PARAM_PORT, ServerConfig.DEFAULT_PORT),
                ConfigurationParser.getMasterInfoOption(options, PARAM_REPLICAOF),
                ConfigurationParser.getLongOption(options, PARAM_REPL_BACKLOG_SIZE,
                        ServerConfig.DEFAULT_REPL_BACKLOG_SIZE),
                ConfigurationParser.getStringOption(options, PARAM_DIR, ServerConfig.DEFAULT_DIR),
                ConfigurationParser.getStringOption(options, PARAM_DBFILENAME, ServerConfig.DEFAULT_DB_FILENAME),
                ConfigurationParser.getBooleanOption(options, PARAM_APPENDONLY, ServerConfig.DEFAULT_APPEND_ONLY),
                ConfigurationParser.getLongOption(options, PARAM_MAXMEMORY, ServerConfig.DEFAULT_MAX_MEMORY),
                ConfigurationParser.getStringOption(options, PARAM_BIND, ServerConfig.DEFAULT_BIND_ADDRESS),
                Optional.ofNullable(options.get(PARAM_REQUIREPASS)),
                ConfigurationParser.getBooleanOption(options, PARAM_HTTP_ENABLED, ServerConfig.DEFAULT_HTTP_ENABLED),
                ConfigurationParser.getIntOption(options, PARAM_HTTP_PORT, ServerConfig.DEFAULT_HTTP_PORT));
    }

    /**
     * Checks if the server is running in replica mode.
     * 
     * @return true if master information is present, false otherwise
     */
    public boolean isReplicaMode() {
        return masterInfo.isPresent();
    }

    /**
     * Gets the master information for replica mode.
     * 
     * @return the master information
     * @throws IllegalStateException if not running in replica mode
     */
    public MasterInfo getMasterInfo() {
        return masterInfo.orElseThrow(() -> new IllegalStateException("Not in replica mode"));
    }

    /**
     * Gets the value of a configuration parameter by name.
     * 
     * @param parameter the parameter name (case-insensitive)
     * @return optional containing the parameter value, or empty if unknown
     *         parameter
     */
    public Optional<String> getConfigParameter(String parameter) {
        return switch (parameter.toLowerCase()) {
            case PARAM_PORT -> Optional.of(String.valueOf(port));
            case PARAM_REPL_BACKLOG_SIZE -> Optional.of(String.valueOf(replicationBacklogSize));
            case PARAM_DIR -> Optional.of(dataDirectory);
            case PARAM_DBFILENAME -> Optional.of(databaseFilename);
            case PARAM_APPENDONLY -> Optional.of(appendOnlyMode ? BOOLEAN_YES : BOOLEAN_NO);
            case PARAM_MAXMEMORY -> Optional.of(String.valueOf(maxMemory));
            case PARAM_BIND -> Optional.of(bindAddress);
            case PARAM_REQUIREPASS -> Optional.of(requirePassword.orElse(EMPTY_STRING));
            case PARAM_MASTER_HOST -> Optional.of(isReplicaMode() ? getMasterInfo().host() : EMPTY_STRING);
            case PARAM_MASTER_PORT ->
                Optional.of(isReplicaMode() ? String.valueOf(getMasterInfo().port()) : DEFAULT_MASTER_PORT_STRING);
            default -> Optional.empty();
        };
    }
}