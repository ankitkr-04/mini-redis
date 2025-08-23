package core;

import blocking.BlockingManager;
import blocking.TimeoutScheduler;
import commands.registry.CommandFactory;
import commands.registry.CommandRegistry;
import events.StorageEventPublisher;
import protocol.CommandDispatcher;
import storage.StorageService;
import transaction.TransactionManager;

public final class ServerContext implements StorageEventPublisher {
    private final StorageService storageService;
    private final BlockingManager blockingManager;
    private final CommandRegistry commandRegistry;
    private final CommandDispatcher commandDispatcher;
    private final TimeoutScheduler timeoutScheduler;
    private final TransactionManager transactionManager;

    public ServerContext() {
        this.storageService = new StorageService();
        this.blockingManager = new BlockingManager(storageService);

        this.transactionManager = new TransactionManager();
        this.commandRegistry =
                CommandFactory.createDefault(this, blockingManager, transactionManager);
        this.commandDispatcher =
                new CommandDispatcher(commandRegistry, storageService, transactionManager);

        this.timeoutScheduler = new TimeoutScheduler(blockingManager);

    }

    public void start() {
        timeoutScheduler.start();
    }

    public void shutdown() {
        timeoutScheduler.shutdown();
        blockingManager.clear();
        storageService.clear();
    }

    // Getters
    public StorageService getStorageService() {
        return storageService;
    }

    public BlockingManager getBlockingManager() {
        return blockingManager;
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public CommandDispatcher getCommandDispatcher() {
        return commandDispatcher;
    }

    // StorageEventPublisher implementation
    @Override
    public void publishDataAdded(String key) {
        blockingManager.onDataAdded(key);
    }

    @Override
    public void publishDataRemoved(String key) {
        blockingManager.onDataRemoved(key);
    }
}
