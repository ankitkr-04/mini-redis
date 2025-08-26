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
    private final PersistentRepository persistentRepository;
    private final File rdbFile;
    private final PubSubManager pubSubManager;
    private final MetricsCollector metricsCollector;
    private final MetricsHandler metricsHandler;
    private final HttpServerManager httpServerManager;

    public ServerContext(ServerConfiguration config) {
        this.config = config;
        rdbFile = new File(config.dataDirectory(), config.databaseFilename());

        this.storageService = new StorageService();
        this.storageService.setEventPublisher(this); // Set event publisher for key modification notifications

        // Initialize persistent repository based on configuration
        if (config.appendOnlyMode()) {
            AofRepository aofRepo = new AofRepository(storageService.getStore(), this);
            aofRepo.setStorageService(storageService);
            persistentRepository = aofRepo;
        } else {
            persistentRepository = new RdbRepository(storageService.getStore());
        }

        this.blockingManager = new BlockingManager(storageService);
        this.transactionManager = new TransactionManager(this);
                this.pubSubManager = new PubSubManager(this);
        // Initialize metrics
        this.metricsCollector = new MetricsCollector();
        this.metricsHandler = new MetricsHandler(metricsCollector);

        // Initialize replication
        this.replicationState = new ReplicationState(
                config.isReplicaMode(),
                config.isReplicaMode() ? config.getMasterInfo().host() : null,
                config.isReplicaMode() ? config.getMasterInfo().port() : 0,
                config.replicationBacklogSize());

        this.replicationManager = new ReplicationManager(replicationState, this);
        this.commandRegistry = CommandFactory.createRegistry(this);
        this.commandDispatcher = new CommandDispatcher(commandRegistry, storageService, transactionManager,
                pubSubManager, this);
        this.timeoutScheduler = new TimeoutScheduler();

        this.replicationClient = config.isReplicaMode()
                ? new ReplicationClient(config.getMasterInfo(), replicationState, config.port(), this)
                : null;

        // Initialize HTTP server if enabled
        this.httpServerManager = config.httpServerEnabled()
                ? new HttpServerManager(this, config.bindAddress(), config.httpPort())
                : null;

        log.info("ServerContext initialized - Port: {}, Role: {}, HTTP: {}",
                config.port(), replicationState.getRole(),
                config.httpServerEnabled() ? "enabled on " + config.httpPort() : "disabled");
    }

    public void start(Selector selector) {
        timeoutScheduler.start();
        blockingManager.start(timeoutScheduler);

        try {
            if (config.appendOnlyMode()) {
                // For AOF mode, use appendonly.aof file
                File aofFile = new File(config.dataDirectory(), "appendonly.aof");
                if (persistentRepository instanceof AofRepository aofRepo) {
                    aofRepo.initializeAofFile(aofFile);
                }
                persistentRepository.loadSnapshot(aofFile);
                log.info("AOF file loaded from {}", aofFile.getAbsolutePath());
            } else {
                // For RDB mode, use the configured database file
                persistentRepository.loadSnapshot(rdbFile);
                log.info("RDB snapshot loaded from {}", rdbFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to load persistence file", e);
        }

        if (replicationClient != null) {
            try {
                replicationClient.register(selector);
                log.info("Replication client registered for master connection");
            } catch (IOException e) {
                log.error("Failed to start replication client", e);
            }
        }

        // Start HTTP server if enabled
        if (httpServerManager != null) {
            try {
                httpServerManager.start();
            } catch (IOException e) {
                log.error("Failed to start HTTP server", e);
            }
        }
    }

    public void shutdown() {
        log.info("Shutting down server context");

        // Stop HTTP server
        if (httpServerManager != null) {
            httpServerManager.stop();
        }

        timeoutScheduler.shutdown();
        if (replicationClient != null) {
            replicationClient.shutdown();
        }
        blockingManager.clear();

        try {
            if (config.appendOnlyMode()) {
                // Close AOF file on shutdown
                var aofRepo = getAofRepository();
                if (aofRepo != null) {
                    aofRepo.close();
                    log.info("AOF file closed");
                }
            } else {
                // Save RDB snapshot on shutdown if not in AOF mode
                persistentRepository.saveSnapshot(rdbFile);
                log.info("RDB snapshot saved to {}", rdbFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to save RDB snapshot on shutdown", e);
        }
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
        return config.appendOnlyMode();
    }

    public File getRdbFile() {
        return rdbFile;
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

    // EventPublisher implementation
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

    // Metrics getters
    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public MetricsHandler getMetricsHandler() {
        return metricsHandler;
    }
}