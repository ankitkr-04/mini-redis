package blocking;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import common.Constants;

public class TimeoutScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("timeout-scheduler").factory());
    private final List<BlockingManager<?>> blockingManagers;

    public TimeoutScheduler(BlockingManager<?>... blockingManagers) {
        this.blockingManagers = Arrays.asList(blockingManagers);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredClients, Constants.CLEANUP_INTERVAL_MS,
                Constants.CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void cleanupExpiredClients() {
        try {
            for (var m : blockingManagers) {
                m.removeExpiredClients();
            }
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
