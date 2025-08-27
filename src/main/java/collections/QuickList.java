package collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.StampedLock;

import config.ProtocolConstants;

/**
 * QuickList is a thread-safe, double-ended queue (deque) implementation using a
 * linked list of fixed-size nodes.
 * It supports efficient push/pop operations from both ends and is optimized for
 * concurrent access.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class QuickList<T> implements Iterable<T> {

    /** Capacity of each node in the QuickList */
    private static final int NODE_CAPACITY = ProtocolConstants.LIST_NODE_CAPACITY;

    /** Default initial index for new nodes (centered for optimal growth) */
    private static final int NODE_INITIAL_INDEX = NODE_CAPACITY / 2;

    /**
     * Node represents a segment in the QuickList, holding a fixed-size array of
     * elements.
     */
    private static final class Node<T> {
        final Object[] elements = new Object[NODE_CAPACITY];
        int start = NODE_INITIAL_INDEX;
        int end = NODE_INITIAL_INDEX;
        Node<T> prev;
        Node<T> next;

        int size() {
            return end - start;
        }

        boolean canGrowLeft() {
            return start > 0;
        }

        boolean canGrowRight() {
            return end < NODE_CAPACITY;
        }

        boolean isEmpty() {
            return start == end;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int totalSize = 0;
    private final StampedLock lock = new StampedLock();

    /**
     * Pushes an element to the left end of the list.
     * 
     * @param value element to add
     */
    public void pushLeft(T value) {
        long stamp = lock.writeLock();
        try {
            ensureHeadCanGrowLeft();
            head.elements[--head.start] = value;
            totalSize++;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Pushes multiple elements to the left end of the list.
     * 
     * @param values elements to add
     */
    @SafeVarargs
    public final void pushLeft(T... values) {
        if (values.length == 0)
            return;
        long stamp = lock.writeLock();
        try {
            for (T value : values) {
                ensureHeadCanGrowLeft();
                head.elements[--head.start] = value;
                totalSize++;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Pushes an element to the right end of the list.
     * 
     * @param value element to add
     */
    public void pushRight(T value) {
        long stamp = lock.writeLock();
        try {
            ensureTailCanGrowRight();
            tail.elements[tail.end++] = value;
            totalSize++;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Pushes multiple elements to the right end of the list.
     * 
     * @param values elements to add
     */
    @SafeVarargs
    public final void pushRight(T... values) {
        if (values.length == 0)
            return;
        long stamp = lock.writeLock();
        try {
            for (T value : values) {
                ensureTailCanGrowRight();
                tail.elements[tail.end++] = value;
                totalSize++;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Pops an element from the left end of the list.
     * 
     * @return the removed element or null if empty
     */
    public T popLeft() {
        long stamp = lock.writeLock();
        try {
            if (head == null)
                return null;
            @SuppressWarnings("unchecked")
            T value = (T) head.elements[head.start];
            head.elements[head.start++] = null;
            totalSize--;
            if (head.isEmpty())
                removeHead();
            return value;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Pops an element from the right end of the list.
     * 
     * @return the removed element or null if empty
     */
    public T popRight() {
        long stamp = lock.writeLock();
        try {
            if (tail == null)
                return null;
            @SuppressWarnings("unchecked")
            T value = (T) tail.elements[--tail.end];
            tail.elements[tail.end] = null;
            totalSize--;
            if (tail.isEmpty())
                removeTail();
            return value;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Pops multiple elements from the left end of the list.
     * 
     * @param count number of elements to pop
     * @return list of removed elements
     */
    public List<T> popLeft(int count) {
        if (count <= 0)
            return List.of();
        long stamp = lock.writeLock();
        try {
            if (count > totalSize)
                throw new IndexOutOfBoundsException(
                        "Cannot pop " + count + " elements, only " + totalSize + " available");
            List<T> result = new ArrayList<>(count);
            int remaining = count;
            while (remaining > 0 && head != null) {
                int take = Math.min(remaining, head.size());
                for (int i = 0; i < take; i++) {
                    @SuppressWarnings("unchecked")
                    T value = (T) head.elements[head.start];
                    result.add(value);
                    head.elements[head.start++] = null;
                }
                if (head.isEmpty())
                    removeHead();
                remaining -= take;
                totalSize -= take;
            }
            return result;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Pops multiple elements from the right end of the list.
     * 
     * @param count number of elements to pop
     * @return list of removed elements
     */
    public List<T> popRight(int count) {
        if (count <= 0)
            return List.of();
        long stamp = lock.writeLock();
        try {
            if (count > totalSize)
                throw new IndexOutOfBoundsException(
                        "Cannot pop " + count + " elements, only " + totalSize + " available");
            List<T> result = new ArrayList<>(count);
            int remaining = count;
            while (remaining > 0 && tail != null) {
                int take = Math.min(remaining, tail.size());
                for (int i = 0; i < take; i++) {
                    @SuppressWarnings("unchecked")
                    T value = (T) tail.elements[--tail.end];
                    result.add(value);
                    tail.elements[tail.end] = null;
                }
                if (tail.isEmpty())
                    removeTail();
                remaining -= take;
                totalSize -= take;
            }
            Collections.reverse(result);
            return result;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns a range of elements from the list.
     * 
     * @param start start index (inclusive)
     * @param end   end index (inclusive)
     * @return list of elements in the specified range
     */
    public List<T> range(int start, int end) {
        long stamp = lock.tryOptimisticRead();
        List<T> result = rangeImpl(start, end);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = rangeImpl(start, end);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    private List<T> rangeImpl(int start, int end) {
        if (head == null || totalSize == 0)
            return List.of();
        if (start < 0)
            start = totalSize + start;
        if (end < 0)
            end = totalSize + end;
        start = Math.max(0, start);
        end = Math.min(totalSize - 1, end);
        if (start > end || start >= totalSize)
            return List.of();
        List<T> result = new ArrayList<>(end - start + 1);
        int currentIndex = 0;
        Node<T> current = head;
        while (current != null && currentIndex <= end) {
            for (int i = current.start; i < current.end && currentIndex <= end; i++, currentIndex++) {
                if (currentIndex >= start) {
                    @SuppressWarnings("unchecked")
                    T value = (T) current.elements[i];
                    result.add(value);
                }

            }
            current = current.next;
        }
        return result;
    }

    /**
     * Returns the total number of elements in the list.
     * Uses optimistic read lock for better performance.
     * 
     * @return number of elements
     */
    public int length() {
        long stamp = lock.tryOptimisticRead();
        int size = totalSize;
        return lock.validate(stamp) ? size : totalSize;
    }

    /**
     * Checks if the list is empty.
     * 
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return length() == 0;
    }

    private void ensureHeadCanGrowLeft() {
        if (head == null) {
            head = tail = new Node<>();
            return;
        }
        if (!head.canGrowLeft()) {
            Node<T> newHead = new Node<>();
            newHead.next = head;
            head.prev = newHead;
            head = newHead;
        }
    }

    private void ensureTailCanGrowRight() {
        if (tail == null) {
            head = tail = new Node<>();
            return;
        }
        if (!tail.canGrowRight()) {
            Node<T> newTail = new Node<>();
            tail.next = newTail;
            newTail.prev = tail;
            tail = newTail;
        }
    }

    private void removeHead() {
        if (head == null)
            return;
        head = head.next;
        if (head != null)
            head.prev = null;
        else
            tail = null;
    }

    private void removeTail() {
        if (tail == null)
            return;
        tail = tail.prev;
        if (tail != null)
            tail.next = null;
        else
            head = null;
    }

    /**
     * Returns a string representation of the list.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        long stamp = lock.readLock();
        try {
            if (isEmpty())
                return "QuickList[]";
            StringBuilder sb = new StringBuilder("QuickList[");
            Node<T> current = head;
            boolean first = true;
            while (current != null) {
                for (int i = current.start; i < current.end; i++) {
                    if (!first)
                        sb.append(", ");
                    @SuppressWarnings("unchecked")
                    T value = (T) current.elements[i];
                    sb.append(value);
                    first = false;
                }
                current = current.next;
            }
            return sb.append("]").toString();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Returns an iterator over the elements in the list.
     * 
     * @return iterator
     */
    @Override
    public Iterator<T> iterator() {
        long stamp = lock.readLock();
        try {
            List<T> snapshot = new ArrayList<>(totalSize);
            Node<T> current = head;
            while (current != null) {
                for (int i = current.start; i < current.end; i++) {
                    @SuppressWarnings("unchecked")
                    T value = (T) current.elements[i];
                    snapshot.add(value);
                }
                current = current.next;
            }
            return snapshot.iterator();
        } finally {
            lock.unlockRead(stamp);
        }
    }
}
