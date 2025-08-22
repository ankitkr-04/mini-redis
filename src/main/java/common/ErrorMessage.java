package common;

/**
 * Centralized error messages for all storage commands. Organized by command/purpose to avoid
 * confusion when extending.
 */
public final class ErrorMessage {
    private ErrorMessage() {}

    /** Messages related to XADD command */
    public static final class XAdd {
        private XAdd() {}


        public static final String ID_MUST_BE_GREATER_THAN_0_0 =
                "The ID specified in XADD must be greater than 0-0";
        public static final String ID_ALREADY_EXISTS = "The ID specified in XADD already exists";
        public static final String ID_EQUAL_OR_SMALLER_THAN_LAST =
                "The ID specified in XADD is equal or smaller than the target stream top item";


    }

    /** Messages related to general stream operations */
    public static final class Stream {
        private Stream() {}

        public static final String INVALID_STREAM_ID_FORMAT = "Invalid stream ID format";
        public static final String STREAM_NOT_FOUND = "The specified stream does not exist";
        public static final String STREAM_EXPIRED = "The stream has expired";
        public static final String MISSING_STREAM_ARGUMENT = "syntax error, missing STREAMS";
        public static final String WRONG_ARGUMENT_COUNT = "wrong number of arguments for XREAD";
    }

    /** Messages related to TTL / Expiry */
    public static final class Expiry {
        private Expiry() {}

        public static final String INVALID_TIMEOUT = "Timeout value is invalid";
    }

    public static final class List {
        private List() {}

        public static final String COUNT_EXCEEDS_LENGTH =
                "List has fewer elements than requested: requested %d, available %d";

        public static final String EMPTY_LIST = "The list '%s' is empty";
    }

    public static final class Command {
        private Command() {}

        public static final String FIELD_VALUE_INCOMPLETE = "Field-value pairs are incomplete";
        public static final String UNSUPPORTED_OPERATION = "Unsupported operation: %s";
        public static final String UNKNOWN_COMMAND = "unknown command";
        public static final String UNKNOWN_COMMAND_WITH_NAME = "unknown command '%s'";
        public static final String WRONG_ARG_COUNT = "wrong number of arguments for '%s' command";
    }

    public static final String UNEXPECTED_TOKEN = "Unexpected token: '%d'";
}
