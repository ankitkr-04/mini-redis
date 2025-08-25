package collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.StampedLock;

import config.ProtocolConstants;

public final class QuickList<T> implements Iterable<T> {
    private static final int NODE_CAPACITY = ProtocolConstants.LIST_NODE_CAPACITY;

    private static final class Node<T> {
        @SuppressWarnings("unchecked")
        final T[] elements = (T[]) new Object[NODE_CAPACITY];
        int start, end; // Range: [start, end)
        Node<T> prev, next;

        Node() {
            start = end = NODE_CAPACITY / 2;
        }

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

    private Node<T> head, tail;
    private int totalSize = 0;
    private final StampedLock lock = new StampedLock();

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

    public T popLeft() {
        long stamp = lock.writeLock();
        try {
            if (head == null)
                return null;
            T value = head.elements[head.start];
            head.elements[head.start++] = null;
            totalSize--;
            if (head.isEmpty())
                removeHead();
            return value;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public T popRight() {
        long stamp = lock.writeLock();
        try {
            if (tail == null)
                return null;
            T value = tail.elements[--tail.end];
            tail.elements[tail.end] = null;
            totalSize--;
            if (tail.isEmpty())
                removeTail();
            return value;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

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
                    result.add(head.elements[head.start]);
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
                    result.add(tail.elements[--tail.end]);
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
            for (int i = current.start; i < current.end && currentIndex <= end; i++) {
                if (currentIndex >= start)
                    result.add(current.elements[i]);
                currentIndex++;
            }
            current = current.next;
        }
        return result;
    }

    /**
     * Returns the total number of elements in the list.
     * Uses optimistic read lock for better performance - if validation fails,
     * falls back to reading the volatile field directly.
     * Thread-safe: This method is lock-free in the common case.
     */
    public int length() {
        long stamp = lock.tryOptimisticRead();
        int size = totalSize;
        return lock.validate(stamp) ? size : totalSize;
    }

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
                    sb.append(current.elements[i]);
                    first = false;
                }
                current = current.next;
            }
            return sb.append("]").toString();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public Iterator<T> iterator() {
        long stamp = lock.readLock();
        try {
            List<T> snapshot = new ArrayList<>(totalSize);
            Node<T> current = head;
            while (current != null) {
                for (int i = current.start; i < current.end; i++) {
                    snapshot.add(current.elements[i]);
                }
                current = current.next;
            }
            return snapshot.iterator();
        } finally {
            lock.unlockRead(stamp);
        }
    }
}
