package server;

import java.io.IOException;
import java.nio.channels.Selector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blocking.BlockingManager;
import blocking.TimeoutScheduler;
import commands.registry.CommandFactory;
import commands.registry.CommandRegistry;
import events.EventPublisher;
import protocol.CommandDispatcher;
import replication.ReplicationClient;
import replication.ReplicationManager;
import replication.ReplicationState;
import storage.StorageService;
import transaction.TransactionManager;

public final class ServerContext implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ServerContext.class);

    private final ServerConfiguration config;
    private final StorageService storageService;
    private final BlockingManager blockingManager;
    private final CommandRegistry commandRegistry;
    private final CommandDispatcher commandDispatcher;
    private final TimeoutScheduler timeoutScheduler;
    private final TransactionManager transactionManager;
    private final ReplicationState replicationState;
    private final ReplicationClient replicationClient;
    private final ReplicationManager replicationManager;

    public ServerContext(ServerConfiguration config) {
        this.config = config;
        this.storageService = new StorageService();
        this.blockingManager = new BlockingManager(storageService);
        this.transactionManager = new TransactionManager();

        // Initialize replication
        this.replicationState = new ReplicationState(
                config.isReplicaMode(),
                config.isReplicaMode() ? config.getMasterInfo().host() : null,
                config.isReplicaMode() ? config.getMasterInfo().port() : 0,
                config.replicationBacklogSize());

        this.replicationManager = new ReplicationManager(replicationState);
        this.commandRegistry = CommandFactory.createRegistry(this);
        this.commandDispatcher = new CommandDispatcher(commandRegistry, storageService, transactionManager, this);
        this.timeoutScheduler = new TimeoutScheduler(blockingManager);

        this.replicationClient = config.isReplicaMode()
                ? new ReplicationClient(config.getMasterInfo(), replicationState, config.port(), this)
                : null;

        log.info("ServerContext initialized - Port: {}, Role: {}",
                config.port(), replicationState.getRole());
    }

    public void start(Selector selector) {
        timeoutScheduler.start();

        if (replicationClient != null) {
            try {
                replicationClient.register(selector);
                log.info("Replication client registered for master connection");
            } catch (IOException e) {
                log.error("Failed to start replication client", e);
            }
        }
    }

    public void shutdown() {
        log.info("Shutting down server context");

        timeoutScheduler.shutdown();
        if (replicationClient != null) {
            replicationClient.shutdown();
        }
        blockingManager.clear();
        storageService.clear();

        log.info("Server context shutdown complete");
    }

    public void propagateWriteCommand(String[] commandArgs) {
        if (replicationState.getConnectedSlaves() > 0) {
            replicationManager.propagateCommand(commandArgs);
        }
    }

    // Getters
    public ServerConfiguration getConfig() {
        return config;
    }

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

    public ReplicationState getReplicationState() {
        return replicationState;
    }

    public ReplicationClient getReplicationClient() {
        return replicationClient;
    }

    public ReplicationManager getReplicationManager() {
        return replicationManager;
    }

    // EventPublisher implementation
    @Override
    public void publishDataAdded(String key) {
        blockingManager.onDataAdded(key);
    }

    @Override
    public void publishDataRemoved(String key) {
        blockingManager.onDataRemoved(key);
    }
}