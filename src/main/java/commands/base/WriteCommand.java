package commands.base;

import server.ServerContext;

public abstract class WriteCommand extends AbstractCommand {
    @Override
    public final boolean isWriteCommand() {
        return true;
    }

    protected void publishDataAdded(String key, ServerContext context) {
        context.publishDataAdded(key);
    }

    protected void propagateCommand(String[] commandArgs, ServerContext context) {
        context.propagateWriteCommand(commandArgs);
    }
}
