package core;

import java.io.IOException;
import java.nio.channels.Selector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blocking.BlockingManager;
import blocking.TimeoutScheduler;
import commands.registry.CommandFactory;
import commands.registry.CommandRegistry;
import events.StorageEventPublisher;
import protocol.CommandDispatcher;
import server.ServerInfo;
import server.ServerOptions;
import server.replication.ReplicationClient;
import server.replication.ReplicationManager;
import storage.StorageService;
import transaction.TransactionManager;

public final class ServerContext implements StorageEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ServerContext.class);

    private final int port;
    private final StorageService storageService;
    private final BlockingManager blockingManager;
    private final CommandRegistry commandRegistry;
    private final CommandDispatcher commandDispatcher;
    private final TimeoutScheduler timeoutScheduler;
    private final TransactionManager transactionManager;
    private final ServerInfo serverInfo;
    private final ReplicationClient replicationClient;
    private final ReplicationManager replicationManager;

    public ServerContext(ServerOptions options) {
        this.port = options.port();
        this.storageService = new StorageService();
        this.blockingManager = new BlockingManager(storageService);
        this.transactionManager = new TransactionManager();
        this.serverInfo = new ServerInfo(options);
        this.replicationManager = new ReplicationManager(serverInfo.getReplicationInfo());
        this.commandRegistry = CommandFactory.createDefault(this);
        this.commandDispatcher = new CommandDispatcher(commandRegistry, storageService,
                transactionManager, this);
        this.timeoutScheduler = new TimeoutScheduler(blockingManager);
        this.replicationClient = options.masterInfo()
                .map(info -> new ReplicationClient(info, serverInfo.getReplicationInfo(), port))
                .orElse(null);

        log.info("ServerContext initialized - Port: {}, Role: {}",
                port, serverInfo.getReplicationInfo().getRole());
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

    // Command propagation for write operations
    public void propagateWriteCommand(String[] commandArgs) {
        if (serverInfo.getReplicationInfo().getConnectedSlaves() > 0) {
            replicationManager.propagateCommand(commandArgs);
        }
    }

    // Getters
    public int getPort() {
        return port;
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

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public ReplicationClient getReplicationClient() {
        return replicationClient;
    }

    public ReplicationManager getReplicationManager() {
        return replicationManager;
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