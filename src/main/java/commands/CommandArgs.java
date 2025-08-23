package commands;

import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record CommandArgs(String operation, String[] rawArgs, SocketChannel clientChannel) {

    // Convenience accessors
    public String key() {
        return hasKey() ? rawArgs[1] : null;
    }

    public String value() {
        return hasValue() ? rawArgs[2] : null;
    }

    public String[] values() {
        return rawArgs.length <= 2 ? new String[0] : Arrays.copyOfRange(rawArgs, 2, rawArgs.length);
    }

    public String arg(int index) {
        return index < rawArgs.length ? rawArgs[index] : null;
    }

    // Validation helpers
    public boolean hasKey() {
        return rawArgs.length >= 2;
    }

    public boolean hasValue() {
        return rawArgs.length >= 3;
    }

    public int argCount() {
        return rawArgs.length;
    }

    // Slice operations using modern Java
    public List<String> slice(int start) {
        return slice(start, rawArgs.length);
    }

    public List<String> slice(int start, int end) {
        if (start < 0 || end > rawArgs.length || start > end) {
            throw new IndexOutOfBoundsException(
                    "Invalid slice range: %d to %d".formatted(start, end));
        }
        return Arrays.asList(rawArgs).subList(start, end);
    }

    // Parsing utilities
    public int getIntArg(int index) {
        return Integer.parseInt(arg(index));
    }

    public long getLongArg(int index) {
        return Long.parseLong(arg(index));
    }

    public double getDoubleArg(int index) {
        return Double.parseDouble(arg(index));
    }

    /**
     * Extracts field-value pairs starting from the given index.
     * Validates that pairs are complete (even count).
     */
    public Map<String, String> fieldValueMap(int startIndex) {
        Map<String, String> map = new HashMap<>();

        if (startIndex >= rawArgs.length) {
            return map;
        }

        int remaining = rawArgs.length - startIndex;
        if (remaining % 2 != 0) {
            throw new IllegalArgumentException("Field-value pairs must be complete");
        }

        for (int i = startIndex; i < rawArgs.length; i += 2) {
            map.put(rawArgs[i], rawArgs[i + 1]);
        }

        return map;
    }

    // Create a copy with different client channel (useful for testing)
    public CommandArgs withClient(SocketChannel newClient) {
        return new CommandArgs(operation, rawArgs, newClient);
    }
}
