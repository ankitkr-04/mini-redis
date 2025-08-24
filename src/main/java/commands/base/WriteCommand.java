package commands.base;

import core.ServerContext;
import events.StorageEventPublisher;

public abstract class WriteCommand extends BaseCommand {
    protected final StorageEventPublisher eventPublisher;
    protected final ServerContext context;

    protected WriteCommand(StorageEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.context = (ServerContext) eventPublisher;
    }

    @Override
    public final boolean requiresClient() {
        return false;
    }

    protected void publishDataAdded(String key) {
        if (eventPublisher != null) {
            eventPublisher.publishDataAdded(key);
        }
    }

    protected void propagateCommand(String[] commandArgs) {
        if (context != null) {
            context.propagateWriteCommand(commandArgs);
        }
    }
}