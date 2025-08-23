package errors;

public enum ErrorCode {
    // Command errors
    UNKNOWN_COMMAND("unknown command '%s'"), WRONG_ARG_COUNT(
            "wrong number of arguments for '%s' command"),

    // Validation errors
    INVALID_INTEGER("value is not an integer or out of range"), INVALID_TIMEOUT(
            "timeout value is invalid"), INVALID_STREAM_ID("Invalid stream ID format"),

    // Storage errors
    WRONG_TYPE("WRONGTYPE Operation against a key holding the wrong kind of value"), KEY_NOT_FOUND(
            "no such key"),

    // Stream specific
    STREAM_ID_TOO_SMALL(
            "The ID specified in XADD is equal or smaller than the target stream top item"), STREAM_ID_EXISTS(
                    "The ID specified in XADD already exists"), STREAM_ID_ZERO(
                            "The ID specified in XADD must be greater than 0-0");

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
