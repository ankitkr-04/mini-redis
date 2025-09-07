package utils;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import config.ServerConfig;

/**
 * High-performance buffer pool for reusing ByteBuffer instances.
 * Reduces allocation overhead by maintaining a pool of pre-allocated buffers.
 * 
 * Features:
 * - Thread-safe buffer recycling
 * - Configurable pool size limits
 * - Automatic buffer clearing on return
 * - Java 24 optimized implementation
 * 
 * @author Ankit Kumar
 * @version 1.0
 */
public final class BufferPool {

    private static final int DEFAULT_BUFFER_SIZE = ServerConfig.BUFFER_SIZE;
    private static final int MAX_POOL_SIZE = ServerConfig.MAX_CONNECTIONS / 2; // Conservative limit

    private final ConcurrentLinkedQueue<ByteBuffer> bufferPool = new ConcurrentLinkedQueue<>();
    private final AtomicInteger poolSize = new AtomicInteger(0);
    private final int bufferSize;
    private final int maxPoolSize;

    private static final BufferPool INSTANCE = new BufferPool();

    private BufferPool() {
        this(DEFAULT_BUFFER_SIZE, MAX_POOL_SIZE);
    }

    private BufferPool(int bufferSize, int maxPoolSize) {
        this.bufferSize = bufferSize;
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * Gets the singleton buffer pool instance.
     * 
     * @return the buffer pool instance
     */
    public static BufferPool getInstance() {
        return INSTANCE;
    }

    /**
     * Acquires a buffer from the pool or creates a new one if pool is empty.
     * 
     * @return a cleared ByteBuffer ready for use
     */
    public ByteBuffer acquire() {
        ByteBuffer buffer = bufferPool.poll();
        if (buffer != null) {
            poolSize.decrementAndGet();
            buffer.clear(); // Ensure buffer is ready for use
            return buffer;
        }

        // Pool is empty, create new buffer
        return ByteBuffer.allocate(bufferSize);
    }

    /**
     * Returns a buffer to the pool for reuse.
     * Buffer will be cleared automatically.
     * 
     * @param buffer the buffer to return (must not be null)
     */
    public void release(ByteBuffer buffer) {
        if (buffer == null || buffer.capacity() != bufferSize) {
            return; // Don't pool buffers of wrong size
        }

        // Only pool if under size limit
        if (poolSize.get() < maxPoolSize) {
            buffer.clear(); // Clear for next use
            bufferPool.offer(buffer);
            poolSize.incrementAndGet();
        }
        // If pool is full, let buffer be GC'd
    }

    /**
     * Gets the current number of buffers in the pool.
     * 
     * @return current pool size
     */
    public int getPoolSize() {
        return poolSize.get();
    }

    /**
     * Gets the maximum pool size.
     * 
     * @return maximum pool size
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Clears all buffers from the pool.
     * Useful for testing or memory cleanup.
     */
    public void clear() {
        bufferPool.clear();
        poolSize.set(0);
    }
}
