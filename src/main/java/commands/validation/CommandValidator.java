package commands.validation;

import commands.context.CommandContext;

public final class CommandValidator {

    public static ValidationResult validateArgCount(CommandContext context, int expectedCount) {
        if (context.getArgCount() != expectedCount) {
            return ValidationResult.invalid("Expected " + expectedCount + " arguments, got " + context.getArgCount());
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateArgRange(CommandContext context, int min, int max) {
        int count = context.getArgCount();
        if (count < min || count > max) {
            return ValidationResult.invalid("Expected " + min + "-" + max + " arguments, got " + count);
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateMinArgs(CommandContext context, int minCount) {
        if (context.getArgCount() < minCount) {
            return ValidationResult
                    .invalid("Expected at least " + minCount + " arguments, got " + context.getArgCount());
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateInteger(String value) {
        try {
            Integer.parseInt(value);
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Invalid integer: " + value);
        }
    }

    public static ValidationResult validateDouble(String value) {
        try {
            Double.parseDouble(value);
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Invalid number: " + value);
        }
    }

    public static ValidationResult validateTimeout(String value) {
        try {
            double timeout = Double.parseDouble(value);
            if (timeout < 0) {
                return ValidationResult.invalid("Timeout cannot be negative");
            }
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Invalid timeout: " + value);
        }
    }

    public static ValidationResult validateStreamId(String id) {
        if (id == null || id.isEmpty()) {
            return ValidationResult.invalid("Stream ID cannot be empty");
        }
        if ("*".equals(id)) {
            return ValidationResult.valid();
        }
        if (id.matches("\\d+-\\d+") || id.matches("\\d+-\\*")) {
            return ValidationResult.valid();
        }
        return ValidationResult.invalid("Invalid stream ID format: " + id);
    }

    /**
     * Validates that arguments from startIndex onwards come in pairs
     * Useful for commands like ZADD, HSET that take key-value pairs
     */
    public static ValidationResult validatePairsAfterIndex(CommandContext context, int startIndex) {
        int remainingArgs = context.getArgCount() - startIndex;
        if (remainingArgs % 2 != 0) {
            return ValidationResult.invalid("Arguments after index " + startIndex + " must come in pairs");
        }
        return ValidationResult.valid();
    }

    /**
     * Validates that all arguments from startIndex onwards are valid integers
     */
    public static ValidationResult validateIntegersAfterIndex(CommandContext context, int startIndex) {
        for (int i = startIndex; i < context.getArgCount(); i++) {
            ValidationResult result = validateInteger(context.getArg(i));
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.valid();
    }

    /**
     * Validates that all arguments from startIndex onwards are valid doubles
     */
    public static ValidationResult validateDoublesAfterIndex(CommandContext context, int startIndex) {
        for (int i = startIndex; i < context.getArgCount(); i++) {
            ValidationResult result = validateDouble(context.getArg(i));
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.valid();
    }

    /**
     * Validates that argument count is odd (useful for commands that take a key
     * plus pairs)
     */
    public static ValidationResult validateOddArgCount(CommandContext context) {
        if (context.getArgCount() % 2 == 0) {
            return ValidationResult.invalid("Expected odd number of arguments");
        }
        return ValidationResult.valid();
    }

    /**
     * Validates that argument count is even (useful for commands that take only
     * pairs)
     */
    public static ValidationResult validateEvenArgCount(CommandContext context) {
        if (context.getArgCount() % 2 != 0) {
            return ValidationResult.invalid("Expected even number of arguments");
        }
        return ValidationResult.valid();
    }
}