package commands.base;

public abstract class ReadCommand extends AbstractCommand {
    @Override
    public final boolean isWriteCommand() {
        return false;
    }
}
