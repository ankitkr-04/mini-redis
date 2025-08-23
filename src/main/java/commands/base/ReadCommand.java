package commands.base;

public abstract class ReadCommand extends BaseCommand {
    @Override
    public final boolean requiresClient() {
        return false;
    }
}