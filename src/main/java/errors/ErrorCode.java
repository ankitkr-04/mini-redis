package errors;

/**
 * Centralized error codes and messages for the Redis server.
 * Uses modern Java features for better maintainability.
 */
public enum ErrorCode {
        // Command errors
        UNKNOWN_COMMAND("unknown command '%s'"), WRONG_ARG_COUNT(
                        "wrong number of arguments for '%s' command"), BLOCKING_IN_TRANSACTION(
                                        "cannot queue blocking commands in transaction"),

        // Validation errors
        INVALID_INTEGER("value is not an integer or out of range"), INVALID_TIMEOUT(
                        "timeout value is invalid"), INVALID_STREAM_ID("Invalid stream ID format"),

        // Storage errors
        WRONG_TYPE("WRONGTYPE Operation against a key holding the wrong kind of value"), KEY_NOT_FOUND(
                        "no such key"),

        // Stream specific errors
        STREAM_ID_TOO_SMALL(
                        "The ID specified in XADD is equal or smaller than the target stream top item"), STREAM_ID_EXISTS(
                                        "The ID specified in XADD already exists"), STREAM_ID_ZERO(
                                                        "The ID specified in XADD must be greater than 0-0"),

        // Transaction errors
        NOT_IN_TRANSACTION("Cannot queue command - not in transaction"), NESTED_MULTI(
                        "MULTI calls can not be nested"), EXEC_WITHOUT_MULTI(
                                        "EXEC without MULTI"), DISCARD_WITHOUT_MULTI(
                                                        "DISCARD without MULTI");

        private final String messageTemplate;

        ErrorCode(String messageTemplate) {
                this.messageTemplate = messageTemplate;
        }

        /**
         * Format the error message with the provided arguments.
         * Uses modern Java String formatting.
         */
        public String format(Object... args) {
                return messageTemplate.formatted(args);
        }

        /**
         * Get the raw message template.
         */
        public String getMessage() {
                return messageTemplate;
        }

        /**
         * Create a formatted error with a specific context.
         */
        public String withContext(String context) {
                return "%s: %s".formatted(context, messageTemplate);

        }
}
