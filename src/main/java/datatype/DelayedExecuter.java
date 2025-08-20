package datatype;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.DelayQueue;

public class DelayedExecuter {
    private final DelayQueue<DelayedTask> delayQueue = new DelayQueue<>();
    private volatile boolean running = true;

    private void procesDelayedTasks() {
        while (running) {
            try {
                DelayedTask task = delayQueue.take();
                Thread.startVirtualThread(task::execute);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

        }

    }

    public void start() {
        Thread.startVirtualThread(this::procesDelayedTasks);
    }

    public void schedule(Runnable task, Duration delay) {
        delayQueue.put(new DelayedTask(task, delay));
    }

    public void scheduleAtTime(Runnable task, Instant executeAt) {
        Duration delay = Duration.between(Instant.now(), executeAt);
        schedule(task, delay);
    }
}
