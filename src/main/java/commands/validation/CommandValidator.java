package commands.validation;

import java.util.function.Predicate;

import commands.context.CommandContext;

/**
 * Provides reusable validation utilities for command arguments.
 * <p>
 * Supports both context-aware validators (using {@link CommandContext})
 * and direct value-based validators (e.g., for integers, doubles, timeouts,
 * etc.).
 * Validators can be composed using {@code and()} / {@code or()} for
 * fluent validation pipelines.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class CommandValidator {

    /** Prevent instantiation of utility class. */
    private CommandValidator() {
    }

    /** Shared error message fragment for argument validation. */
    private static final String ARGUMENTS_GOT = " arguments, got ";

    /**
     * Functional interface for context-aware command validation.
     */
    @FunctionalInterface
    public interface CommandValidation {
        /**
         * Validates the given command context.
         *
         * @param context the command execution context
         * @return the validation result
         */
        ValidationResult validate(CommandContext context);

        /**
         * Chains this validation with another one, requiring both to pass.
         */
        default CommandValidation and(CommandValidation other) {
            return ctx -> {
                ValidationResult result = this.validate(ctx);
                return result.isValid() ? other.validate(ctx) : result;
            };
        }

        /**
         * Chains this validation with another one, requiring at least one to pass.
         */
        default CommandValidation or(CommandValidation other) {
            return ctx -> {
                ValidationResult result = this.validate(ctx);
                return result.isValid() ? result : other.validate(ctx);
            };
        }
    }

    // ----------------------
    // Context-based validators
    // ----------------------

    /**
     * Applies a validation only when a condition on the context is true.
     */
    public static CommandValidation when(Predicate<CommandContext> condition,
            CommandValidation thenValidation) {
        return ctx -> condition.test(ctx)
                ? thenValidation.validate(ctx)
                : ValidationResult.valid();
    }

    /**
     * Validates that the argument count matches the expected value.
     */
    public static CommandValidation argCount(int expectedCount) {
        return context -> {
            int actualCount = context.getArgCount();
            if (actualCount != expectedCount) {
                return ValidationResult.invalid(
                        "Expected " + expectedCount + ARGUMENTS_GOT + actualCount);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Validates that the argument count falls within the given range.
     */
    public static CommandValidation argRange(int minCount, int maxCount) {
        return context -> {
            int actualCount = context.getArgCount();
            if (actualCount < minCount || actualCount > maxCount) {
                return ValidationResult.invalid(
                        "Expected " + minCount + "-" + maxCount + ARGUMENTS_GOT + actualCount);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Validates that there are at least {@code minCount} arguments.
     */
    public static CommandValidation minArgs(int minCount) {
        return context -> {
            int actualCount = context.getArgCount();
            if (actualCount < minCount) {
                return ValidationResult.invalid(
                        "Expected at least " + minCount + ARGUMENTS_GOT + actualCount);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Validates that arguments after the given index appear in pairs.
     */
    public static CommandValidation pairsAfter(int startIndex) {
        return context -> {
            int remainingArgs = context.getArgCount() - startIndex;
            if (remainingArgs % 2 != 0) {
                return ValidationResult.invalid(
                        "Arguments after index " + startIndex + " must come in pairs");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Validates that the argument count is odd.
     */
    public static CommandValidation oddArgCount() {
        return context -> context.getArgCount() % 2 == 0
                ? ValidationResult.invalid("Expected odd number of arguments")
                : ValidationResult.valid();
    }

    /**
     * Validates that the argument count is even.
     */
    public static CommandValidation evenArgCount() {
        return context -> context.getArgCount() % 2 != 0
                ? ValidationResult.invalid("Expected even number of arguments")
                : ValidationResult.valid();
    }

    /**
     * Validates that the argument at the given index matches the expected string.
     */
    public static CommandValidation argEquals(int index, String expectedValue) {
        return context -> {
            int argCount = context.getArgCount();
            if (index >= argCount) {
                return ValidationResult.invalid("Argument index " + index + " out of bounds");
            }
            String actualValue = context.getArg(index);
            if (!expectedValue.equalsIgnoreCase(actualValue)) {
                return ValidationResult.invalid(
                        "Expected argument at index " + index + " to be '" + expectedValue + "'");
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Validates that arguments at the given indexes are integers.
     */
    public static CommandValidation intArg(int... indexes) {
        return context -> {
            for (int index : indexes) {
                ValidationResult result = validateInteger(context.getArg(index));
                if (!result.isValid()) {
                    return result;
                }
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Validates that the argument at the given index is a double.
     */
    public static CommandValidation doubleArg(int index) {
        return context -> validateDouble(context.getArg(index));
    }

    /**
     * Validates that the argument at the given index is a valid timeout.
     */
    public static CommandValidation timeoutArg(int index) {
        return context -> validateTimeout(context.getArg(index));
    }

    /**
     * Validates that the argument at the given index is a valid stream ID.
     */
    public static CommandValidation streamIdArg(int index) {
        return context -> validateStreamId(context.getArg(index));
    }

    // ----------------------
    // Non-context validators
    // ----------------------

    /**
     * Validates that a string can be parsed as an integer.
     */
    public static ValidationResult validateInteger(String value) {
        try {
            Integer.parseInt(value);
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Invalid integer: " + value);
        }
    }

    /**
     * Validates that a string can be parsed as a double.
     */
    public static ValidationResult validateDouble(String value) {
        try {
            Double.parseDouble(value);
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Invalid number: " + value);
        }
    }

    /**
     * Validates that a string can be parsed as a non-negative timeout value.
     */
    public static ValidationResult validateTimeout(String value) {
        try {
            double timeout = Double.parseDouble(value);
            return timeout < 0
                    ? ValidationResult.invalid("Timeout cannot be negative")
                    : ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("Invalid timeout: " + value);
        }
    }

    /**
     * Validates that a string is a valid stream ID.
     */
    public static ValidationResult validateStreamId(String id) {
        if (id == null || id.isEmpty()) {
            return ValidationResult.invalid("Stream ID cannot be empty");
        }
        if ("*".equals(id) || id.matches("\\d+-\\d+") || id.matches("\\d+-\\*")) {
            return ValidationResult.valid();
        }
        return ValidationResult.invalid("Invalid stream ID format: " + id);
    }
}
