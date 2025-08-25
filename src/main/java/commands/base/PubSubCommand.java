package commands.base;

public abstract class PubSubCommand extends AbstractCommand {
    @Override
    public final boolean requiresClient() {
        return true; // must be tied to a client connection
    }

    @Override
    public boolean isPubSubCommand() {
        return true;
    }
}
