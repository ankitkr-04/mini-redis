package storage.types.streams;

import java.util.Map;

/**
 * Represents an entry in a stream, consisting of a unique identifier and a set
 * of fields.
 *
 * @param id     the unique identifier for the stream entry
 * @param fields a map containing the field names and their corresponding values
 *               for this entry
 */
public record StreamEntry(String id, Map<String, String> fields) {
}
