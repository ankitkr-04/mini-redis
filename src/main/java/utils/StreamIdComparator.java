package utils;

import java.util.Comparator;

public final class StreamIdComparator implements Comparator<String> {
    public static final StreamIdComparator INSTANCE = new StreamIdComparator();

    private StreamIdComparator() {}

    @Override
    public int compare(String id1, String id2) {
        try {
            String[] parts1 = id1.split("-");
            String[] parts2 = id2.split("-");

            long ms1 = Long.parseLong(parts1[0]);
            long ms2 = Long.parseLong(parts2[0]);

            if (ms1 != ms2) {
                return Long.compare(ms1, ms2);
            }

            long seq1 = Long.parseLong(parts1[1]);
            long seq2 = Long.parseLong(parts2[1]);

            return Long.compare(seq1, seq2);
        } catch (Exception e) {
            // Fallback to string comparison
            return id1.compareTo(id2);
        }
    }

    public static int compareIds(String id1, String id2) {
        return INSTANCE.compare(id1, id2);
    }
}
