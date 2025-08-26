package commands.base;

/**
 * Represents a Redis command that blocks the client until a condition is met or
 * a timeout occurs.
 * <p>
 * This abstract class should be extended by commands that require blocking
 * behavior.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public abstract class BlockingCommand extends AbstractCommand {

    /** Indicates that this command requires a client context */
    protected static final boolean REQUIRES_CLIENT = true;

    /** Indicates that this command is a blocking command */
    protected static final boolean IS_BLOCKING_COMMAND = true;

    /**
     * Indicates whether this command requires a client context.
     *
     * @return true if the command requires a client, false otherwise
     */
    @Override
    public final boolean requiresClient() {
        return REQUIRES_CLIENT;
    }

    /**
     * Indicates whether this command is a blocking command.
     *
     * @return true if the command is blocking, false otherwise
     */
    @Override
    public final boolean isBlockingCommand() {
        return IS_BLOCKING_COMMAND;
    }
}