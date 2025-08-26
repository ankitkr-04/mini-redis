package commands.base;

/**
 * Abstract base class for Pub/Sub commands in the Redis protocol.
 * <p>
 * Ensures that all Pub/Sub commands are associated with a client connection.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public abstract class PubSubCommand extends AbstractCommand {

    /**
     * Indicates that this command requires a client connection.
     *
     * @return true, as Pub/Sub commands must be tied to a client connection
     */
    @Override
    public final boolean requiresClient() {
        return true;
    }

    /**
     * Identifies this command as a Pub/Sub command.
     *
     * @return true, as this is a Pub/Sub command
     */
    @Override
    public boolean isPubSubCommand() {
        return true;
    }
}