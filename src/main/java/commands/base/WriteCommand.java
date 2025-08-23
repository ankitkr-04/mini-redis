package commands.base;

import events.StorageEventPublisher;

public abstract class WriteCommand extends BaseCommand {
    protected final StorageEventPublisher eventPublisher;

    protected WriteCommand(StorageEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
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
}
