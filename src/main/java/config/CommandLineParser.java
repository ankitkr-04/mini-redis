package config;

import java.util.HashMap;
import java.util.Map;

public final class CommandLineParser {
    private CommandLineParser() {} // Utility class

    public static Map<String, String> parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2).toLowerCase(); // Remove --, normalize
                String value = args[i + 1];
                if (value.startsWith("--")) {
                    throw new IllegalArgumentException("Missing value for option: " + args[i]);
                }
                options.put(key, value);
                i++; // Skip value
            }
        }
        return options;
    }

    public static int getIntOption(Map<String, String> options, String key, int defaultValue) {
        String value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println(
                    "Invalid " + key + " value: " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    public static String getStringOption(Map<String, String> options, String key,
            String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }
}
