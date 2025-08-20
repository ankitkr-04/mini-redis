package commands;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import common.ErrorMessage;

public record CommandArgs(String operation, String[] rawArgs, SocketChannel clientChannel) {

    public String key() {
        return hasKey() ? rawArgs[1] : null;
    }

    public String value() {
        return hasValue() ? rawArgs[2] : null;
    }

    public String[] values() {
        if (rawArgs.length <= 2)
            return new String[0];
        String[] values = new String[rawArgs.length - 2];
        System.arraycopy(rawArgs, 2, values, 0, values.length);
        return values;
    }

    /**
     * Extracts a map of field-value pairs starting from a given index. Ensures pairs are complete
     * (even length).
     */
    public Map<String, String> fieldValueMap(int startIndex) {
        Map<String, String> map = new HashMap<>();
        if (startIndex >= rawArgs.length)
            return map;

        // must be pairs
        if (((rawArgs.length - startIndex) % 2) != 0) {
            throw new IllegalArgumentException(ErrorMessage.Command.FIELD_VALUE_INCOMPLETE);
        }

        for (int i = startIndex; i < rawArgs.length; i += 2) {
            String field = rawArgs[i];
            String value = rawArgs[i + 1];
            map.put(field, value);
        }
        return map;
    }

    public String arg(int index) {
        return index < rawArgs.length ? rawArgs[index] : null;
    }

    public boolean hasKey() {
        return rawArgs.length >= 2;
    }

    public boolean hasValue() {
        return rawArgs.length >= 3;
    }

    public int argCount() {
        return rawArgs.length;
    }
}
