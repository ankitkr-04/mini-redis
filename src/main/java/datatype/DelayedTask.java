package datatype;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedTask implements Delayed {
    private final Runnable task;
    private final long executeAtNanos;

    public DelayedTask(Runnable task, Duration delay) {
        this.task = task;
        this.executeAtNanos = System.nanoTime() + delay.toNanos();
    }


    @Override
    public long getDelay(TimeUnit unit) {
        long remaining = executeAtNanos - System.nanoTime();
        return unit.convert(remaining, TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.executeAtNanos, ((DelayedTask) other).executeAtNanos);
    }

    public void execute() {
        task.run();
    }

}
