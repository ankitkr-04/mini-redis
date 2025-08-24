package protocol.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CommandLineParser {
    private CommandLineParser() {} // Utility class

    // Define valid option keys
    private static final Set<String> VALID_OPTIONS = Set.of(
            "port", "replicaof", "repl-backlog-size", "dir", "dbfilename",
            "appendonly", "maxmemory", "bind", "requirepass");

    // Generic parser interface
    private interface OptionParser<T> {
        T parse(String value, String key) throws IllegalArgumentException;
    }

    public static class ParseResult {
        private final Map<String, String> options;
        private final Map<String, String> errors;

        private ParseResult(Map<String, String> options, Map<String, String> errors) {
            this.options = options;
            this.errors = errors;
        }

        public Map<String, String> getOptions() {
            return options;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }

    public static ParseResult parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2).toLowerCase();
                if (!VALID_OPTIONS.contains(key)) {
                    errors.put(key, "Unknown option: " + key);
                    continue;
                }
                if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                    errors.put(key, "Missing value for option: " + key);
                    continue;
                }
                options.put(key, args[++i]);
            }
        }
        return new ParseResult(options, errors);
    }

    private static <T> T parseOption(Map<String, String> options, String key, T defaultValue,
            OptionParser<T> parser) {
        String value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return parser.parse(value, key);
        } catch (IllegalArgumentException e) {
            logError(e.getMessage());
            return defaultValue;
        }
    }

    public static int getIntOption(Map<String, String> options, String key, int defaultValue) {
        return parseOption(options, key, defaultValue, (value, k) -> {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid %s value: %s. Using default: %d"
                        .formatted(k, value, defaultValue));
            }
        });
    }

    public static long getLongOption(Map<String, String> options, String key, long defaultValue) {
        return parseOption(options, key, defaultValue, (value, k) -> {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid %s value: %s. Using default: %d"
                        .formatted(k, value, defaultValue));
            }
        });
    }

    public static String getStringOption(Map<String, String> options, String key,
            String defaultValue) {
        return parseOption(options, key, defaultValue, (value, _) -> value);
    }

    public static boolean getBooleanOption(Map<String, String> options, String key,
            boolean defaultValue) {
        return parseOption(options, key, defaultValue, (value, k) -> {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException(
                        "Invalid %s value: %s. Expected 'true' or 'false'. Using default: %b"
                                .formatted(k, value, defaultValue));
            }
            return Boolean.parseBoolean(value);
        });
    }

    public static Optional<MasterInfo> getMasterInfoOption(Map<String, String> options,
            String key) {
        return parseOption(options, key, Optional.empty(), (value, k) -> {
            String[] parts = value.trim().split("\\s+");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid --%s format: %s. Expected 'host port'.".formatted(k, value));
            }
            String host = parts[0];
            int port;
            try {
                port = Integer.parseInt(parts[1]);
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException(
                            "Invalid port in --%s: %s. Must be between 1 and 65535.".formatted(k,
                                    parts[1]));
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid port in --%s: %s. Must be a valid integer.".formatted(k,
                                parts[1]));
            }
            return Optional.of(new MasterInfo(host, port));
        });
    }

    private static void logError(String msg) {
        System.err.println(msg);
    }

    public record MasterInfo(String host, int port) {
    }
}
