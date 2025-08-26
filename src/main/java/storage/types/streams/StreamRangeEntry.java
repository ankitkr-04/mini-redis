package storage.types.streams;

import java.util.List;

/**
 * Represents a single entry in a stream range, containing an entry ID and a
 * list of fields.
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public record StreamRangeEntry(String entryId, List<String> fields) {
    // No additional logic required for this record.
}
