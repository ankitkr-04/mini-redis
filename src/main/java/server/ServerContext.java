package server;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Selector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blocking.BlockingManager;
import commands.registry.CommandFactory;
import commands.registry.CommandRegistry;
import events.EventPublisher;
import metrics.MetricsCollector;
import metrics.MetricsHandler;
import protocol.CommandDispatcher;
import pubsub.PubSubManager;
import replication.ReplicationClient;
import replication.ReplicationManager;
import replication.ReplicationState;
import scheduler.TimeoutScheduler;
import server.http.HttpServerManager;
import storage.StorageService;
import storage.persistence.AofRepository;
import storage.persistence.PersistentRepository;
import storage.persistence.RdbRepository;
import transaction.TransactionManager;

/**
 * Core server context responsible for initializing and managing all
 * subsystems (storage, persistence, replication, pub/sub, HTTP, etc.).
 *
 * <p>
 * Provides a central access point for shared resources and coordinates
 * server startup and shutdown.
 * </p>
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class ServerContext implements EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerContext.class);

    /** Default filename for Append-Only File persistence. */
    private static final String AOF_FILENAME = "appendonly.aof";

    private final ServerConfiguration serverConfig;
    private final StorageService storageService;
    private final BlockingManager blockingManager;
    private final CommandRegistry commandRegistry;
    private final CommandDispatcher commandDispatcher;
    private final TimeoutScheduler timeoutScheduler;
    private final TransactionManager transactionManager;
    private final ReplicationState replicationState;
    private final ReplicationClient replicationClient;
    private final ReplicationManager replicationManager;
    private final PersistentRepository persistentRepository;
    private final File rdbSnapshotFile;
    private final PubSubManager pubSubManager;
    private final MetricsCollector metricsCollector;
    private final MetricsHandler metricsHandler;
    private final HttpServerManager httpServerManager;

    /**
     * Creates and initializes the server context.
     *
     * @param serverConfig the configuration used to set up server components
     */
    public ServerContext(ServerConfiguration serverConfig) {
        this.serverConfig = serverConfig;
        this.rdbSnapshotFile = new File(serverConfig.dataDirectory(), serverConfig.databaseFilename());

        this.storageService = new StorageService();
        this.storageService.setEventPublisher(this);

        this.persistentRepository = serverConfig.appendOnlyMode()
                ? initAofRepository()
                : new RdbRepository(storageService.getStore());

        this.blockingManager = new BlockingManager(storageService);
        this.transactionManager = new TransactionManager(this);
        this.pubSubManager = new PubSubManager(this);

        this.metricsCollector = new MetricsCollector();
        this.metricsHandler = new MetricsHandler(metricsCollector);

        this.replicationState = new ReplicationState(
                serverConfig.isReplicaMode(),
                serverConfig.isReplicaMode() ? serverConfig.getMasterInfo().host() : null,
                serverConfig.isReplicaMode() ? serverConfig.getMasterInfo().port() : 0,
                serverConfig.replicationBacklogSize());

        this.replicationManager = new ReplicationManager(replicationState, this);
        this.commandRegistry = CommandFactory.createRegistry(this);
        this.commandDispatcher = new CommandDispatcher(commandRegistry, storageService, transactionManager,
                pubSubManager, this);
        this.timeoutScheduler = new TimeoutScheduler();

        this.replicationClient = serverConfig.isReplicaMode()
                ? new ReplicationClient(serverConfig.getMasterInfo(), replicationState, serverConfig.port(), this)
                : null;

        this.httpServerManager = serverConfig.httpServerEnabled()
                ? new HttpServerManager(this, serverConfig.bindAddress(), serverConfig.httpPort())
                : null;

        LOGGER.info("ServerContext initialized - Port: {}, Role: {}, HTTP: {}",
                serverConfig.port(), replicationState.getRole(),
                serverConfig.httpServerEnabled() ? "enabled on " + serverConfig.httpPort() : "disabled");
    }

    private PersistentRepository initAofRepository() {
        AofRepository aofRepository = new AofRepository(storageService.getStore(), this);
        aofRepository.setStorageService(storageService);
        return aofRepository;
    }

    /**
     * Starts all server components (scheduler, persistence, replication, HTTP).
     *
     * @param selector the NIO selector for replication client registration
     */
    public void start(Selector selector) {
        timeoutScheduler.start();
        blockingManager.start(timeoutScheduler);

        loadPersistence();

        if (replicationClient != null) {
            try {
                replicationClient.register(selector);
                LOGGER.info("Replication client registered for master connection");
            } catch (IOException e) {
                LOGGER.error("Failed to start replication client", e);
            }
        }

        if (httpServerManager != null) {
            try {
                httpServerManager.start();
            } catch (IOException e) {
                LOGGER.error("Failed to start HTTP server", e);
            }
        }
    }

    private void loadPersistence() {
        try {
            if (serverConfig.appendOnlyMode()) {
                File aofFile = new File(serverConfig.dataDirectory(), AOF_FILENAME);
                if (persistentRepository instanceof AofRepository aofRepo) {
                    aofRepo.initializeAofFile(aofFile);
                }
                persistentRepository.loadSnapshot(aofFile);
                LOGGER.info("AOF file loaded from {}", aofFile.getAbsolutePath());
            } else {
                persistentRepository.loadSnapshot(rdbSnapshotFile);
                LOGGER.info("RDB snapshot loaded from {}", rdbSnapshotFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load persistence file", e);
        }
    }

    /**
     * Gracefully shuts down the server and all managed components.
     */
    public void shutdown() {
        LOGGER.info("Shutting down server context");

        if (httpServerManager != null) {
            httpServerManager.stop();
        }

        timeoutScheduler.shutdown();
        if (replicationClient != null) {
            replicationClient.shutdown();
        }
        blockingManager.clear();

        savePersistenceOnShutdown();
        storageService.clear();

        LOGGER.info("Server context shutdown complete");
    }

    private void savePersistenceOnShutdown() {
        try {
            if (serverConfig.appendOnlyMode()) {
                AofRepository aofRepository = getAofRepository();
                if (aofRepository != null) {
                    aofRepository.close();
                    LOGGER.info("AOF file closed");
                }
            } else {
                persistentRepository.saveSnapshot(rdbSnapshotFile);
                LOGGER.info("RDB snapshot saved to {}", rdbSnapshotFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save persistence on shutdown", e);
        }
    }

    public void propagateWriteCommand(String[] commandArgs) {
        if (replicationState.getConnectedSlaves() > 0) {
            replicationManager.propagateCommand(commandArgs);
        }
    }

    // ---- Getters ----
    public ServerConfiguration getConfig() {
        return serverConfig;
    }

    public PubSubManager getPubSubManager() {
        return pubSubManager;
    }

    public PersistentRepository getPersistentRepository() {
        return persistentRepository;
    }

    public AofRepository getAofRepository() {
        return persistentRepository instanceof AofRepository aofRepo ? aofRepo : null;
    }

    public boolean isAofMode() {
        return serverConfig.appendOnlyMode();
    }

    public File getRdbFile() {
        return rdbSnapshotFile;
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

    public TimeoutScheduler getTimeoutScheduler() {
        return timeoutScheduler;
    }

    public ReplicationManager getReplicationManager() {
        return replicationManager;
    }

    public HttpServerManager getHttpServerManager() {
        return httpServerManager;
    }

    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public MetricsHandler getMetricsHandler() {
        return metricsHandler;
    }

    // ---- EventPublisher implementation ----
    @Override
    public void publishDataAdded(String key) {
        blockingManager.onDataAdded(key);
    }

    @Override
    public void publishDataRemoved(String key) {
        blockingManager.onDataRemoved(key);
    }

    @Override
    public void publishKeyModified(String key) {
        transactionManager.invalidateWatchingClients(key);
    }

    @Override
    public void publishStoreCleared() {
        LOGGER.info("All keys cleared from store");

        blockingManager.onStoreCleared();
        transactionManager.invalidateAllWatchingClients();

        // Clear AOF
        AofRepository aof = getAofRepository();
        if (aof != null) {
            try {
                aof.clear(); // implement clear in AofRepository
                LOGGER.info("AOF cleared due to FLUSHALL");
            } catch (Exception e) {
                LOGGER.error("Failed to clear AOF during FLUSHALL", e);
            }
        }
    }

    @Override
    public void publishExpiredKeysRemoved(int count) {
        metricsCollector.incrementExpiredKeys(count); // or loop increment if only single increment API exists
        LOGGER.debug("{} expired keys removed during cleanup", count);
    }

}
