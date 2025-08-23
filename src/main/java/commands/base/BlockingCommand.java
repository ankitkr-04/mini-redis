package commands.base;

public abstract class BlockingCommand extends BaseCommand {
    @Override
    public final boolean requiresClient() {
        return true;
    }
}
