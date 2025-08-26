package scheduler;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.ServerConfig;

/**
 * Schedules and executes timeout tasks at a fixed interval.
 * 
 * This class manages delayed tasks and periodically executes them using a
 * single-threaded scheduled executor.
 */
public final class TimeoutScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutScheduler.class);

    private static final String THREAD_NAME = "timeout-scheduler";
    private static final long SHUTDOWN_AWAIT_SECONDS = 1L;

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name(THREAD_NAME).factory());
    private final ConcurrentLinkedDeque<Runnable> pendingTasks = new ConcurrentLinkedDeque<>();

    /**
     * Starts the periodic execution of pending tasks.
     */
    public void start() {
        scheduledExecutor.scheduleAtFixedRate(
                this::executePendingTasks,
                ServerConfig.CLEANUP_INTERVAL_MS,
                ServerConfig.CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        LOGGER.info("TimeoutScheduler started with interval {} ms", ServerConfig.CLEANUP_INTERVAL_MS);
    }

    /**
     * Schedules a task to be executed after a specified delay.
     *
     * @param delayMs the delay in milliseconds before executing the task
     * @param task    the task to execute
     */
    public void schedule(long delayMs, Runnable task) {
        pendingTasks.add(() -> {
            if (delayMs <= 0) {
                task.run();
            } else {
                scheduledExecutor.schedule(task, delayMs, TimeUnit.MILLISECONDS);
            }
        });
    }

    /**
     * Executes all pending tasks in the queue.
     */
    private void executePendingTasks() {
        Runnable task;
        while ((task = pendingTasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception ex) {
                LOGGER.warn("Exception while executing scheduled task", ex);
            }
        }
    }

    /**
     * Shuts down the scheduler gracefully, waiting for tasks to complete.
     */
    public void shutdown() {
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
                LOGGER.info("TimeoutScheduler forced shutdown after timeout.");
            } else {
                LOGGER.info("TimeoutScheduler shutdown completed.");
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.warn("TimeoutScheduler interrupted during shutdown.", e);
        }
    }
}
