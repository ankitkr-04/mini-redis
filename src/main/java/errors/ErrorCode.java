package errors;

/**
 * Centralized error codes and messages for the Redis server.
 * Uses modern Java features for better maintainability.
 */
public enum ErrorCode {
        // Command errors
        UNKNOWN_COMMAND("unknown command '%s'"), WRONG_ARG_COUNT(
                        "wrong number of arguments for '%s' command"),
        BLOCKING_IN_TRANSACTION(
                        "cannot queue blocking commands in transaction"),

        // Validation errors
        INVALID_INTEGER("value is not an integer or out of range"), INVALID_TIMEOUT(
                        "timeout value is invalid"),
        INVALID_STREAM_ID("Invalid stream ID format"),

        // Storage errors
        WRONG_TYPE("WRONGTYPE Operation against a key holding the wrong kind of value"), KEY_NOT_FOUND(
                        "no such key"),

        // Stream specific errors
        STREAM_ID_TOO_SMALL(
                        "The ID specified in XADD is equal or smaller than the target stream top item"),
        STREAM_ID_EXISTS(
                        "The ID specified in XADD already exists"),
        STREAM_ID_ZERO(
                        "The ID specified in XADD must be greater than 0-0"),

        // Transaction errors
        NOT_IN_TRANSACTION("Cannot queue command - not in transaction"), NESTED_MULTI(
                        "MULTI calls can not be nested"),
        EXEC_WITHOUT_MULTI(
                        "EXEC without MULTI"),
        DISCARD_WITHOUT_MULTI(
                        "DISCARD without MULTI"),
        WATCH_INSIDE_MULTI("WATCH inside MULTI is not allowed"), UNWATCH_WITHOUT_WATCH(
                        "UNWATCH without WATCH"),

        // Replication errors
        REPLICA_NOT_CONNECTED("Replica is not connected"), REPLICA_ALREADY_CONNECTED(
                        "Replica is already connected"),
        MASTER_NOT_CONFIGURED(
                        "No master is configured"),
        REPLICA_OF_SELF(
                        "Cannot replicate from self"),
        REPLICA_SYNC_IN_PROGRESS(
                        "Replica synchronization in progress"),
        // PubSubManager
        PUBSUB_INVALID_CHANNEL("Invalid channel name"), PUBSUB_NOT_SUBSCRIBED(
                        "Not subscribed to the specified channel"),
        PUBSUB_ALREADY_SUBSCRIBED(
                        "Already subscribed to the specified channel"),
        NOT_ALLOWED_IN_PUBSUB_MODE(
                        "Can't execute '%s': only (P|S)SUBSCRIBE / (P|S)UNSUBSCRIBE / PING / QUIT / RESET are allowed in this context");

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
