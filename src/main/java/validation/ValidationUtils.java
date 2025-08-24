package validation;

import commands.CommandArgs;

public final class ValidationUtils {
    private ValidationUtils() {
    }

    public static ValidationResult validateArgCount(CommandArgs args, int expected) {
        return args.argCount() == expected
                ? ValidationResult.valid()
                : ValidationResult.invalid(String.format(
                        "wrong number of arguments for '%s' command", args.operation()));
    }

    public static ValidationResult validateArgRange(CommandArgs args, int min, int max) {
        int count = args.argCount();
        return (count >= min && count <= max)
                ? ValidationResult.valid()
                : ValidationResult.invalid(String.format(
                        "wrong number of arguments for '%s' command", args.operation()));
    }

    public static ValidationResult validateMinArgs(CommandArgs args, int min) {
        return args.argCount() >= min
                ? ValidationResult.valid()
                : ValidationResult.invalid(String.format(
                        "wrong number of arguments for '%s' command", args.operation()));
    }

    public static ValidationResult validateInteger(String value) {
        try {
            Integer.parseInt(value);
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("value is not an integer");
        }
    }

    public static ValidationResult validateLong(String value) {
        try {
            Long.parseLong(value);
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("value is not a valid long");
        }
    }

    public static ValidationResult validateTimeout(String value) {
        try {
            double timeout = Double.parseDouble(value);
            return timeout >= 0
                    ? ValidationResult.valid()
                    : ValidationResult.invalid("timeout must be non-negative");
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("invalid timeout value");
        }
    }

    public static ValidationResult validateStreamId(String id) {
        return ("*".equals(id) || id.endsWith("-*") || id.matches("\\d+-\\d+"))
                ? ValidationResult.valid()
                : ValidationResult.invalid("Invalid stream ID format");
    }
}