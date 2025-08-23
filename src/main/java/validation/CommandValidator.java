package validation;

import commands.CommandArgs;
import errors.ErrorCode;
import errors.ValidationError;

public final class CommandValidator {
    private CommandValidator() {} // Utility class

    public static ValidationResult validateArgCount(CommandArgs args, int expected) {
        if (args.argCount() != expected) {
            return ValidationResult.invalid(
                    new ValidationError(
                            ErrorCode.WRONG_ARG_COUNT.format(args.operation()),
                            ErrorCode.WRONG_ARG_COUNT));
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateArgRange(CommandArgs args, int min, int max) {
        int count = args.argCount();
        if (count < min || count > max) {
            return ValidationResult.invalid(
                    new ValidationError(
                            ErrorCode.WRONG_ARG_COUNT.format(args.operation()),
                            ErrorCode.WRONG_ARG_COUNT));
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateInteger(String value) {
        try {
            Integer.parseInt(value);
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid(
                    new ValidationError(
                            ErrorCode.INVALID_INTEGER.getMessage(),
                            ErrorCode.INVALID_INTEGER));
        }
    }

    public static ValidationResult validateTimeout(String value) {
        try {
            double timeout = Double.parseDouble(value);
            if (timeout < 0) {
                return ValidationResult.invalid(
                        new ValidationError(
                                ErrorCode.INVALID_TIMEOUT.getMessage(),
                                ErrorCode.INVALID_TIMEOUT));
            }
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid(
                    new ValidationError(
                            ErrorCode.INVALID_TIMEOUT.getMessage(),
                            ErrorCode.INVALID_TIMEOUT));
        }
    }

    public static ValidationResult validateStreamId(String id) {
        if ("*".equals(id) || id.endsWith("-*")) {
            return ValidationResult.valid();
        }

        if (!id.matches("\\d+-\\d+")) {
            return ValidationResult.invalid(
                    new ValidationError(
                            ErrorCode.INVALID_STREAM_ID.getMessage(),
                            ErrorCode.INVALID_STREAM_ID));
        }

        return ValidationResult.valid();
    }
}
