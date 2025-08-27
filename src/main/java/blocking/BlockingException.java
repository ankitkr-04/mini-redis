package blocking;

/**
 * Exception thrown when blocking operations fail in the BlockingManager.
 * 
 * This exception indicates that a client could not be blocked due to
 * various reasons such as:
 * - Network/channel issues
 * - Invalid client state
 * - Resource constraints
 * - Configuration problems
 * - Internal blocking system failures
 * 
 * This is a runtime exception to avoid forcing callers to handle
 * blocking failures in normal operation flow.
 */
public class BlockingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new blocking exception with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public BlockingException(String message) {
        super(message);
    }

    /**
     * Constructs a new blocking exception with the specified detail message
     * and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause   the underlying cause of this exception
     */
    public BlockingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new blocking exception with the specified cause.
     * The detail message will be derived from the cause.
     *
     * @param cause the underlying cause of this exception
     */
    public BlockingException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new blocking exception with no detail message.
     * This constructor should be used sparingly as it provides
     * limited debugging information.
     */
    public BlockingException() {
        super();
    }
}