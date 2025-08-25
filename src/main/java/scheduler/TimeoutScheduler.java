package scheduler;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import config.ServerConfig;

public final class TimeoutScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("timeout-scheduler").factory());
    private final ConcurrentLinkedDeque<Runnable> tasks = new ConcurrentLinkedDeque<>();

    public TimeoutScheduler() {

    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::runPendingTasks,
                ServerConfig.CLEANUP_INTERVAL_MS,
                ServerConfig.CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    public void schedule(long delayMs, Runnable task) {
        tasks.add(() -> {
            if (delayMs <= 0) {
                task.run();
            } else {
                scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
            }
        });
    }

    private void runPendingTasks() {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            task.run();
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
