package utils;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comparator for Redis stream IDs in the format "milliseconds-sequence".
 * <p>
 * Compares two stream IDs by their millisecond and sequence components.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */

public final class StreamIdComparator implements Comparator<String> {

    /**
     * Logger instance for this class.
     * Only use for important events or errors during comparison.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamIdComparator.class);

    /**
     * Delimiter used to split stream IDs (milliseconds-sequence).
     */
    private static final String STREAM_ID_DELIMITER = "-";

    public static final StreamIdComparator INSTANCE = new StreamIdComparator();

    private StreamIdComparator() {
    }

    @Override

    /**
     * Compares two stream IDs by their millisecond and sequence components.
     * Falls back to string comparison if parsing fails.
     *
     * @param id1 the first stream ID
     * @param id2 the second stream ID
     * @return negative if id1 < id2, positive if id1 > id2, zero if equal
     */
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
            // If parsing fails, fallback to string comparison
            LOGGER.debug("Failed to parse stream IDs '{}', '{}': {}. Falling back to string comparison.", id1, id2,
                    e.getMessage());
            return id1.compareTo(id2);
        }
    }

    /**
     * Static utility to compare two stream IDs.
     *
     * @param id1 the first stream ID
     * @param id2 the second stream ID
     * @return comparison result as per {@link #compare(String, String)}
     */
    public static int compareIds(String id1, String id2) {
        return INSTANCE.compare(id1, id2);
    }
}
