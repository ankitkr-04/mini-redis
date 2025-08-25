package config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ConfigurationParser {
    private static final Set<String> VALID_OPTIONS = Set.of(
            "port", "replicaof", "repl-backlog-size", "dir", "dbfilename",
            "appendonly", "maxmemory", "bind", "requirepass", "http-enabled", "http-port");

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

    public static int getIntOption(Map<String, String> options, String key, int defaultValue) {
        String value = options.get(key);
        if (value == null)
            return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid " + key + " value: " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static long getLongOption(Map<String, String> options, String key, long defaultValue) {
        String value = options.get(key);
        if (value == null)
            return defaultValue;

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid " + key + " value: " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static String getStringOption(Map<String, String> options, String key, String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }

    public static boolean getBooleanOption(Map<String, String> options, String key, boolean defaultValue) {
        String value = options.get(key);
        if (value == null)
            return defaultValue;

        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            System.err.println("Invalid " + key + " value: " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public static Optional<MasterInfo> getMasterInfoOption(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null)
            return Optional.empty();

        String[] parts = value.trim().split("\\s+");
        if (parts.length != 2) {
            System.err.println("Invalid " + key + " format: " + value + ". Expected 'host port'.");
            return Optional.empty();
        }

        try {
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            if (port < 1 || port > 65535) {
                System.err.println("Invalid port in " + key + ": " + parts[1] + ". Must be between 1 and 65535.");
                return Optional.empty();
            }
            return Optional.of(new MasterInfo(host, port));
        } catch (NumberFormatException e) {
            System.err.println("Invalid port in " + key + ": " + parts[1] + ". Must be a valid integer.");
            return Optional.empty();
        }
    }

    public record ParseResult(Map<String, String> options, Map<String, String> errors) {
    }

    public record MasterInfo(String host, int port) {
    }
}