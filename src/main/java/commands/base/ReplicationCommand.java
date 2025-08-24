package commands.base;

public abstract class ReplicationCommand extends AbstractCommand {
    @Override
    public final boolean isReplicationCommand() {
        return true;
    }

}
