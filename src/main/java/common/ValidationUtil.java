package common;

public final class ValidationUtil {
    private ValidationUtil() {}

    public static boolean isValidTimeout(String timeoutStr) {
        try {
            double timeout = Double.parseDouble(timeoutStr);
            return timeout >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean hasMinArgs(String[] args, int minCount) {
        return args != null && args.length >= minCount;
    }


    public static boolean isValidStreamId(String id) {
        if ("*".equals(id))
            return true;
        String[] parts = id.split("-");
        if (parts.length != 2)
            return false;
        try {
            long ms = Long.parseLong(parts[0]);
            long seq = Long.parseLong(parts[1]);
            return ms >= 0 && seq >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int compareStreamIds(String id1, String id2) {
        String[] p1 = id1.split("-");
        String[] p2 = id2.split("-");
        long ms1 = Long.parseLong(p1[0]);
        long ms2 = Long.parseLong(p2[0]);
        if (ms1 != ms2)
            return Long.compare(ms1, ms2);
        long s1 = Long.parseLong(p1[1]);
        long s2 = Long.parseLong(p2[1]);
        return Long.compare(s1, s2);
    }


}
