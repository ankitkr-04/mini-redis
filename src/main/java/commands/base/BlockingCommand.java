package commands.base;

public abstract class BlockingCommand extends AbstractCommand {
    @Override
    public final boolean requiresClient() {
        return true;
    }

    @Override
    public final boolean isBlockingCommand() {
        return true;
    }
}