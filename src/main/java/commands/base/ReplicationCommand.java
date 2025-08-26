package commands.base;

/**
 * Abstract base class for replication-related commands in the Redis protocol.
 * <p>
 * Subclasses represent commands that are involved in replication.
 * </p>
 */
public abstract class ReplicationCommand extends AbstractCommand {

    /**
     * Indicates that this command is related to replication.
     *
     * @return true, as this is a replication command
     */
    @Override
    public final boolean isReplicationCommand() {
        return true;
    }

}
