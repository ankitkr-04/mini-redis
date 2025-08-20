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

}
