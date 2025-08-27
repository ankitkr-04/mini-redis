package utils;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comparator for Redis stream IDs in the format "milliseconds-sequence".
 * Compares two stream IDs by their millisecond and sequence components.
 */
public enum StreamIdComparator implements Comparator<String> {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamIdComparator.class);
    private static final String STREAM_ID_DELIMITER = "-";

    @Override
    public int compare(String id1, String id2) {
        try {
            String[] parts1 = id1.split(STREAM_ID_DELIMITER);
            String[] parts2 = id2.split(STREAM_ID_DELIMITER);

            long ms1 = Long.parseLong(parts1[0]);
            long ms2 = Long.parseLong(parts2[0]);
            if (ms1 != ms2) {
                return Long.compare(ms1, ms2);
            }

            long seq1 = Long.parseLong(parts1[1]);
            long seq2 = Long.parseLong(parts2[1]);
            return Long.compare(seq1, seq2);
        } catch (Exception e) {
            LOGGER.debug("Failed to parse stream IDs '{}', '{}': {}. Falling back to string comparison.",
                    id1, id2, e.getMessage());
            return id1.compareTo(id2);
        }
    }

    /** Static utility to compare two stream IDs. */
    public static int compareIds(String id1, String id2) {
        return INSTANCE.compare(id1, id2);
    }
}
