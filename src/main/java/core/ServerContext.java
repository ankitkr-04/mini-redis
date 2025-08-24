package core;

import java.io.IOException;
import java.nio.channels.Selector;
import blocking.BlockingManager;
import blocking.TimeoutScheduler;
import commands.registry.CommandFactory;
import commands.registry.CommandRegistry;
import events.StorageEventPublisher;
import protocol.CommandDispatcher;
import server.ServerInfo;
import server.ServerOptions;
import server.replication.ReplicationClient;
import storage.StorageService;
import transaction.TransactionManager;

public final class ServerContext implements StorageEventPublisher {
    private final int port;

    private final StorageService storageService;
    private final BlockingManager blockingManager;
    private final CommandRegistry commandRegistry;
    private final CommandDispatcher commandDispatcher;
    private final TimeoutScheduler timeoutScheduler;
    private final TransactionManager transactionManager;
    private final ServerInfo serverInfo;
    private final ReplicationClient replicationClient;

    public ServerContext(ServerOptions options) {
        this.port = options.port();
        this.storageService = new StorageService();
        this.blockingManager = new BlockingManager(storageService);

        this.transactionManager = new TransactionManager();
        this.serverInfo = new ServerInfo(options);
        this.commandRegistry = CommandFactory.createDefault(this);
        this.commandDispatcher =
                new CommandDispatcher(commandRegistry, storageService, transactionManager);

        this.timeoutScheduler = new TimeoutScheduler(blockingManager);
        this.replicationClient =
                options.masterInfo()
                        .map(info -> new ReplicationClient(info, serverInfo.getReplicationInfo(),
                                port))
                        .orElse(null);
    }

    public int getPort() {
        return port;
    }

    public void start(Selector selector) {
        timeoutScheduler.start();
        if (replicationClient != null) {
            try {
                replicationClient.register(selector);
                System.out.println("Replication client registered for master connection");
            } catch (IOException e) {
                System.err.println("Failed to start replication client: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        timeoutScheduler.shutdown();
        if (replicationClient != null) {
            replicationClient.shutdown();
        }
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

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public ReplicationClient getReplicationClient() {
        return replicationClient;
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
