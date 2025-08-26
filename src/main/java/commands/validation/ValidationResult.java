package commands.validation;

/**
 * Represents the result of a command validation.
 * <p>
 * Provides information about whether a command is valid and, if not, the
 * associated error message.
 * </p>
 */
public final class ValidationResult {

    private static final String DEFAULT_ERROR_MESSAGE = "Validation failed";

    private final boolean isValid;
    private final String errorMessage;

    private ValidationResult(boolean isValid, String errorMessage) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a ValidationResult representing a successful validation.
     *
     * @return a valid ValidationResult instance
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    /**
     * Creates a ValidationResult representing a failed validation.
     *
     * @param errorMessage the error message describing why validation failed
     * @return an invalid ValidationResult instance
     */
    public static ValidationResult invalid(String errorMessage) {
        String message = (errorMessage == null || errorMessage.isEmpty()) ? DEFAULT_ERROR_MESSAGE : errorMessage;
        return new ValidationResult(false, message);
    }

    /**
     * Indicates whether the validation was successful.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Returns the error message if validation failed.
     *
     * @return the error message, or null if valid
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
