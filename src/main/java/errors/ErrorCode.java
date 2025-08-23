package errors;

public enum ErrorCode {

        // Command errors
        UNKNOWN_COMMAND("unknown command '%s'"),
        // Error for unknown or unsupported commands
        WRONG_ARG_COUNT("wrong number of arguments for '%s' command"),
        // Error for incorrect argument count
        BLOCKNG_IN_TRANSACTION("cannot queue blocking commands in transaction"),
        // Error for queuing blocking commands in a transaction

        // Validation errors
        INVALID_INTEGER("value is not an integer or out of range"),

        // Error for invalid integer values
        INVALID_TIMEOUT("timeout value is invalid"),
        // Error for invalid timeout values
        INVALID_STREAM_ID("Invalid stream ID format"),
        // Error for invalid stream ID format

        // Storage errors
        WRONG_TYPE("WRONGTYPE Operation against a key holding the wrong kind of value"),
        // Error for operations on keys of the wrong type
        KEY_NOT_FOUND("no such key"),
        // Error for accessing a non-existent key

        // Stream specific
        STREAM_ID_TOO_SMALL(
                        "The ID specified in XADD is equal or smaller than the target stream top item"),
        // Error for stream ID being too small
        STREAM_ID_EXISTS("The ID specified in XADD already exists"),
        // Error for duplicate stream ID
        STREAM_ID_ZERO("The ID specified in XADD must be greater than 0-0"),
        // Error for stream ID being zero
        NOT_IN_TRANSACTION("Cannot queue command - not in transaction"),
        // Error for queuing commands outside a transaction
        NESTED_MULTI("MULTI calls can not be nested"),
        // Error for nested MULTI calls
        EXEC_WITHOUT_MULTI("EXEC without MULTI"),
        // Error for EXEC command without a preceding MULTI
        DISCARD_WITHOUT_MULTI("DISCARD without MULTI");
        // Error for DISCARD command without a preceding MULTI

        private final String messageTemplate;

        ErrorCode(String messageTemplate) {
                this.messageTemplate = messageTemplate;
        }

        public String format(Object... args) {
                return String.format(messageTemplate, args);
        }

        public String getMessage() {
                return messageTemplate;
        }
}
