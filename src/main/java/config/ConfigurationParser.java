package config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced configuration parser with improved error handling, validation,
 * and use of centralized constants from ServerConfig.
 */
public final class ConfigurationParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParser.class);
    private static final String OPTION_MAP_CANNOT_BE_NULL = "Options map cannot be null";
    private static final String KEY_CANNOT_BE_NULL = "Key cannot be null";

    // Valid configuration options
    private static final Set<String> VALID_OPTIONS = Set.of(
            "port", "replicaof", "repl-backlog-size", "dir", "dbfilename",
            "appendonly", "maxmemory", "bind", "requirepass", "http-enabled", "http-port");

    private ConfigurationParser() {
        // Utility class - prevent instantiation
    }

    /**
     * Parses command line arguments into configuration options with validation.
     * Handles null args gracefully (common in Redis CLI startup).
     * 
     * @param args command line arguments (can be null)
     * @return ParseResult containing parsed options and any errors
     */
    public static ParseResult parse(String[] args) {
        // Handle null args gracefully - Redis can start without arguments
        if (args == null || args.length == 0) {
            return new ParseResult(Map.of(), Map.of());
        }

        final Map<String, String> options = new HashMap<>();
        final Map<String, String> errors = new HashMap<>();
        int i = 0;
        while (i < args.length) {
            final String arg = args[i];

            if (arg.startsWith(ServerConfig.OPTION_PREFIX)) {
                final String key = arg.substring(ServerConfig.OPTION_PREFIX_LENGTH).toLowerCase();

                if (!VALID_OPTIONS.contains(key)) {
                    final String errorMsg = String.format("Unknown option: %s", key);
                    errors.put(key, errorMsg);
                    LOGGER.warn(errorMsg);
                    i++;
                } else if (i + 1 >= args.length || args[i + 1].startsWith(ServerConfig.OPTION_PREFIX)) {
                    final String errorMsg = String.format("Missing value for option: %s", key);
                    errors.put(key, errorMsg);
                    LOGGER.warn(errorMsg);
                    i++;
                } else {
                    options.put(key, args[i + 1]);
                    i += 2; // safely skip the value
                }
            } else {
                i++;
            }
        }

        return new ParseResult(Map.copyOf(options), Map.copyOf(errors));
    }

    /**
     * Gets an integer option with validation and default fallback.
     * 
     * @param options      the parsed options map
     * @param key          the option key
     * @param defaultValue the default value to use if parsing fails
     * @return the parsed integer value or default
     */
    public static int getIntOption(Map<String, String> options, String key, int defaultValue) {
        Objects.requireNonNull(options, OPTION_MAP_CANNOT_BE_NULL);
        Objects.requireNonNull(key, KEY_CANNOT_BE_NULL);

        final String value = options.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            final String errorMsg = String.format("Invalid %s value: %s. Using default: %d",
                    key, value, defaultValue);
            LOGGER.warn(errorMsg);
            return defaultValue;
        }
    }

    /**
     * Gets a long option with validation and default fallback.
     * 
     * @param options      the parsed options map
     * @param key          the option key
     * @param defaultValue the default value to use if parsing fails
     * @return the parsed long value or default
     */
    public static long getLongOption(Map<String, String> options, String key, long defaultValue) {
        Objects.requireNonNull(options, OPTION_MAP_CANNOT_BE_NULL);
        Objects.requireNonNull(key, KEY_CANNOT_BE_NULL);

        final String value = options.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            final String errorMsg = String.format("Invalid %s value: %s. Using default: %d",
                    key, value, defaultValue);
            LOGGER.warn(errorMsg);
            return defaultValue;
        }
    }

    /**
     * Gets a string option with trimming and default fallback.
     * 
     * @param options      the parsed options map
     * @param key          the option key
     * @param defaultValue the default value to use if option is missing
     * @return the string value or default
     */
    public static String getStringOption(Map<String, String> options, String key, String defaultValue) {
        Objects.requireNonNull(options, OPTION_MAP_CANNOT_BE_NULL);
        Objects.requireNonNull(key, KEY_CANNOT_BE_NULL);

        final String value = options.get(key);
        return value != null ? value.trim() : defaultValue;
    }

    /**
     * Gets a boolean option with validation and default fallback.
     * 
     * @param options      the parsed options map
     * @param key          the option key
     * @param defaultValue the default value to use if parsing fails
     * @return the parsed boolean value or default
     */
    public static boolean getBooleanOption(Map<String, String> options, String key, boolean defaultValue) {
        Objects.requireNonNull(options, OPTION_MAP_CANNOT_BE_NULL);
        Objects.requireNonNull(key, KEY_CANNOT_BE_NULL);

        final String value = options.get(key);
        if (value == null) {
            return defaultValue;
        }

        final String trimmedValue = value.trim().toLowerCase();
        if (!"true".equals(trimmedValue) && !"false".equals(trimmedValue)) {
            final String errorMsg = String.format("Invalid %s value: %s. Using default: %s",
                    key, value, defaultValue);
            LOGGER.warn(errorMsg);
            return defaultValue;
        }

        return Boolean.parseBoolean(trimmedValue);
    }

    /**
     * Parses master info from replicaof option with enhanced validation.
     * 
     * @param options the parsed options map
     * @param key     the option key (typically "replicaof")
     * @return Optional containing MasterInfo if valid, empty otherwise
     */
    public static Optional<MasterInfo> getMasterInfoOption(Map<String, String> options, String key) {
        Objects.requireNonNull(options, OPTION_MAP_CANNOT_BE_NULL);
        Objects.requireNonNull(key, KEY_CANNOT_BE_NULL);

        final String value = options.get(key);
        if (value == null) {
            return Optional.empty();
        }

        final String[] parts = value.trim().split(ServerConfig.MASTER_INFO_DELIMITER);
        if (parts.length != ServerConfig.MASTER_INFO_PARTS_COUNT) {
            final String errorMsg = String.format("Invalid %s format: %s. Expected 'host port'", key, value);
            LOGGER.warn(errorMsg);
            return Optional.empty();
        }

        try {
            final String host = parts[0].trim();
            final int port = Integer.parseInt(parts[1].trim());

            if (!isValidPort(port)) {
                final String errorMsg = String.format("Invalid port in %s: %d. Must be between %d and %d",
                        key, port, ServerConfig.MIN_PORT_VALUE, ServerConfig.MAX_PORT_VALUE);
                LOGGER.warn(errorMsg);
                return Optional.empty();
            }

            if (host.isEmpty()) {
                final String errorMsg = String.format("Invalid host in %s: host cannot be empty", key);
                LOGGER.warn(errorMsg);
                return Optional.empty();
            }

            return Optional.of(new MasterInfo(host, port));

        } catch (NumberFormatException e) {
            final String errorMsg = String.format("Invalid port in %s: %s. Must be a valid integer",
                    key, parts[1]);
            LOGGER.warn(errorMsg);
            return Optional.empty();
        }
    }

    /**
     * Validates if a port number is within acceptable range.
     * 
     * @param port the port number to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidPort(int port) {
        return port >= ServerConfig.MIN_PORT_VALUE && port <= ServerConfig.MAX_PORT_VALUE;
    }

    /**
     * Immutable record representing parsing results.
     * 
     * @param options parsed configuration options
     * @param errors  any parsing errors encountered
     */
    public record ParseResult(Map<String, String> options, Map<String, String> errors) {

        public ParseResult {
            Objects.requireNonNull(options, "Options cannot be null");
            Objects.requireNonNull(errors, "Errors cannot be null");
        }

        /**
         * Checks if parsing was successful (no errors).
         * 
         * @return true if no errors, false otherwise
         */
        public boolean isSuccessful() {
            return errors.isEmpty();
        }

        /**
         * Gets the number of parsed options.
         * 
         * @return option count
         */
        public int getOptionCount() {
            return options.size();
        }

        /**
         * Gets the number of parsing errors.
         * 
         * @return error count
         */
        public int getErrorCount() {
            return errors.size();
        }
    }

    /**
     * Immutable record representing master server information.
     * 
     * @param host the master host
     * @param port the master port
     */
    public record MasterInfo(String host, int port) {

        public MasterInfo {
            Objects.requireNonNull(host, "Host cannot be null");
            if (host.trim().isEmpty()) {
                throw new IllegalArgumentException("Host cannot be empty");
            }
            if (!isValidPort(port)) {
                throw new IllegalArgumentException(
                        String.format("Port must be between %d and %d, got: %d",
                                ServerConfig.MIN_PORT_VALUE, ServerConfig.MAX_PORT_VALUE, port));
            }
        }

        /**
         * Creates a connection string for this master info.
         * 
         * @return formatted connection string
         */
        public String getConnectionString() {
            return String.format("%s:%d", host, port);
        }

        /**
         * Checks if this represents a localhost connection.
         * 
         * @return true if host is localhost, false otherwise
         */
        public boolean isLocalhost() {
            return "localhost".equalsIgnoreCase(host) ||
                    "127.0.0.1".equals(host) ||
                    "::1".equals(host);
        }
    }
}