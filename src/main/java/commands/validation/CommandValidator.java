package commands.validation;

import java.util.function.Predicate;

import commands.context.CommandContext;
import utils.GeoUtils;

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
        default CommandValidation and(final CommandValidation other) {
            return ctx -> {
                final ValidationResult result = this.validate(ctx);
                return result.isValid() ? other.validate(ctx) : result;
            };
        }

        /**
         * Chains this validation with another one, requiring at least one to pass.
         */
        default CommandValidation or(final CommandValidation other) {
            return ctx -> {
                final ValidationResult result = this.validate(ctx);
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
    public static CommandValidation when(final Predicate<CommandContext> condition,
            final CommandValidation thenValidation) {
        return ctx -> condition.test(ctx)
                ? thenValidation.validate(ctx)
                : ValidationResult.valid();
    }

    /**
     * Validates that the argument count matches the expected value.
     */
    public static CommandValidation argCount(final int expectedCount) {
        return context -> {
            final int actualCount = context.getArgCount();
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
    public static CommandValidation argRange(final int minCount, final int maxCount) {
        return context -> {
            final int actualCount = context.getArgCount();
            if (actualCount < minCount || actualCount > maxCount) {
                return ValidationResult.invalid(
                        "Expected " + minCount + "-" + maxCount + ARGUMENTS_GOT + actualCount);
            }
            return ValidationResult.valid();
        };
    }

    /**
     * Validates that coordinates (longitude, latitude) appear in triplets
     * starting from the given index.
     * <p>
     * Each triplet consists of longitude, latitude, and a place name.
     * Longitude must be between -180 and 180.
     * Latitude must be between -85.05112878 and 85.05112878.
     * </p>
     *
     * @param startIndex the index where the first longitude appears
     * @return a CommandValidation that checks coordinate triplets
     */
    public static CommandValidation validateCoordinates(final int startIndex) {
        return context -> {
            final int argCount = context.getArgCount();

            // Must be divisible into triplets
            if ((argCount - startIndex) % 3 != 0) {
                return ValidationResult.invalid(
                        "Arguments after index " + startIndex +
                                " must come in triplets of longitude, latitude, and member");
            }

            for (int i = startIndex; i < argCount; i += 3) {
                try {
                    final double longitude = Double.parseDouble(context.getArg(i));
                    final double latitude = Double.parseDouble(context.getArg(i + 1));
                    // Member = context.getArg(i + 2), no validation needed here

                    if (!GeoUtils.isValidLongitude(longitude)) {
                        return ValidationResult.invalid(
                                "longitude value (" + longitude + ") is invalid");
                    }
                    if (!GeoUtils.isValidLatitude(latitude)) {
                        return ValidationResult.invalid(
                                "latitude value (" + latitude + ") is invalid");
                    }
                } catch (final NumberFormatException e) {
                    return ValidationResult.invalid(
                            "invalid number format for longitude/latitude at index " + i);
                }
            }

            return ValidationResult.valid();
        };
    }

    /**
     * Validates that there are at least {@code minCount} arguments.
     */
    public static CommandValidation minArgs(final int minCount) {
        return context -> {
            final int actualCount = context.getArgCount();
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
    public static CommandValidation pairsAfter(final int startIndex) {
        return context -> {
            final int remainingArgs = context.getArgCount() - startIndex;
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
    public static CommandValidation argEquals(final int index, final String expectedValue) {
        return context -> {
            final int argCount = context.getArgCount();
            if (index >= argCount) {
                return ValidationResult.invalid("Argument index " + index + " out of bounds");
            }
            final String actualValue = context.getArg(index);
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
    public static CommandValidation intArg(final int... indexes) {
        return context -> {
            for (final int index : indexes) {
                final ValidationResult result = validateInteger(context.getArg(index));
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
    public static CommandValidation doubleArg(final int index) {
        return context -> validateDouble(context.getArg(index));
    }

    /**
     * Validates that the argument at the given index is a valid timeout.
     */
    public static CommandValidation timeoutArg(final int index) {
        return context -> validateTimeout(context.getArg(index));
    }

    /**
     * Validates that the argument at the given index is a valid stream ID.
     */
    public static CommandValidation streamIdArg(final int index) {
        return context -> validateStreamId(context.getArg(index));
    }

    // ----------------------
    // Non-context validators
    // ----------------------

    /**
     * Validates that a string can be parsed as an integer.
     */
    public static ValidationResult validateInteger(final String value) {
        try {
            Integer.parseInt(value);
            return ValidationResult.valid();
        } catch (final NumberFormatException e) {
            return ValidationResult.invalid("Invalid integer: " + value);
        }
    }

    /**
     * Validates that a string can be parsed as a double.
     */
    public static ValidationResult validateDouble(final String value) {
        try {
            Double.parseDouble(value);
            return ValidationResult.valid();
        } catch (final NumberFormatException e) {
            return ValidationResult.invalid("Invalid number: " + value);
        }
    }

    /**
     * Validates that a string can be parsed as a non-negative timeout value.
     */
    public static ValidationResult validateTimeout(final String value) {
        try {
            final double timeout = Double.parseDouble(value);
            return timeout < 0
                    ? ValidationResult.invalid("Timeout cannot be negative")
                    : ValidationResult.valid();
        } catch (final NumberFormatException e) {
            return ValidationResult.invalid("Invalid timeout: " + value);
        }
    }

    /**
     * Validates that a string is a valid stream ID.
     */
    public static ValidationResult validateStreamId(final String id) {
        if (id == null || id.isEmpty()) {
            return ValidationResult.invalid("Stream ID cannot be empty");
        }
        if ("*".equals(id) || id.matches("\\d+-\\d+") || id.matches("\\d+-\\*")) {
            return ValidationResult.valid();
        }
        return ValidationResult.invalid("Invalid stream ID format: " + id);
    }
}
