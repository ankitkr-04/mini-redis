package commands.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.ServerContext;

/**
 * Abstract base class for write commands in the Redis protocol.
 * <p>
 * Provides utility methods for publishing data changes and propagating write
 * commands.
 * </p>
 */
public abstract class WriteCommand extends AbstractCommand {

    /** Logger for WriteCommand operations */
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteCommand.class);

    /** Indicates that this command is a write operation. */
    @Override
    public final boolean isWriteCommand() {
        return true;
    }

    /**
     * Publishes an event indicating that data has been added for the specified key.
     *
     * @param key     the key for which data was added
     * @param context the server context
     */
    protected void publishDataAdded(String key, ServerContext context) {
        context.publishDataAdded(key);
        LOGGER.debug("Data added event published for key: {}", key);
    }

    /**
     * Propagates the write command to other nodes or replicas.
     *
     * @param commandArgs the command arguments
     * @param context     the server context
     */
    protected void propagateCommand(String[] commandArgs, ServerContext context) {
        context.propagateWriteCommand(commandArgs);
        LOGGER.trace("Write command propagated: {}", (Object) commandArgs);
    }
}
