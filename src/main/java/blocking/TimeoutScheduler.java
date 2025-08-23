package blocking;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import config.ServerConfig;

public final class TimeoutScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("timeout-scheduler").factory());
    private final BlockingManager blockingManager;

    public TimeoutScheduler(BlockingManager blockingManager) {
        this.blockingManager = blockingManager;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::cleanupExpiredClients,
                ServerConfig.CLEANUP_INTERVAL_MS,
                ServerConfig.CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    private void cleanupExpiredClients() {
        try {
            blockingManager.removeExpiredClients();
        } catch (Exception e) {
            System.err.println("Error during timeout cleanup: " + e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
