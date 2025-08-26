package commands.base;

/**
 * ReadCommand implementation.
 *
 * <p>
 * Command implementation for Redis protocol.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */

/**
 * Abstract base class for all read-only Redis commands.
 * <p>
 * Ensures that implementing commands are recognized as read operations.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 * @since 1.0
 */
public abstract class ReadCommand extends AbstractCommand {

    /** Indicates that this command is a read operation (not a write). */
    private static final boolean IS_WRITE_COMMAND = false;

    @Override
    public final boolean isWriteCommand() {
        return IS_WRITE_COMMAND;
    }
}
