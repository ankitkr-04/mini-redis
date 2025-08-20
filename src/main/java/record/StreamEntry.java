package record;

import java.util.LinkedHashMap;
import java.util.Map;

public record StreamEntry(String id, Map<String, String> fields) {
    public StreamEntry {
        fields = Map.copyOf(fields);
    }

    public static StreamEntry fromKeyValuePairs(String id, String[] keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must have even number of elements");
        }

        Map<String, String> fields = new LinkedHashMap<>();

        for (int i = 0; i < keyValuePairs.length; i += 2) {
            fields.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }

        return new StreamEntry(id, fields);

    }


}
