package datatype;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class QuickList<T> {
    private static final int NODE_CAPACITY = 64;

    private static class Node<T> {
        T[] elements;
        int start = 0, end = 0; // [start, end) valid elements
        Node<T> prev, next;

        @SuppressWarnings("unchecked")
        Node() {
            elements = (T[]) new Object[NODE_CAPACITY];
        }

        int size() {
            return end - start;
        }
    }

    private Node<T> head, tail;
    private final AtomicInteger size = new AtomicInteger(0);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // ---------------- Single element push ----------------
    public void pushLeft(T val) {
        lock.writeLock().lock();
        try {
            if (head == null)
                head = tail = new Node<>();
            if (head.size() >= NODE_CAPACITY) {
                Node<T> newHead = new Node<>();
                newHead.next = head;
                head.prev = newHead;
                head = newHead;
            }
            head.start--;
            head.elements[head.start] = val;
            size.incrementAndGet();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void pushRight(T val) {
        lock.writeLock().lock();
        try {
            if (tail == null)
                head = tail = new Node<>();
            if (tail.size() >= NODE_CAPACITY) {
                Node<T> newTail = new Node<>();
                tail.next = newTail;
                newTail.prev = tail;
                tail = newTail;
            }
            tail.elements[tail.end++] = val;
            size.incrementAndGet();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void pushLeft(T... vals) {
        for (T val : vals)
            pushLeft(val);
    }

    public void pushRight(T... vals) {
        for (T val : vals)
            pushRight(val);
    }


    // ---------------- Single element pop ----------------
    public T popLeft() {
        lock.writeLock().lock();
        try {
            if (head == null)
                return null;
            T val = head.elements[head.start++];
            if (head.start == head.end)
                removeHead();
            size.decrementAndGet();
            return val;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public T popRight() {
        lock.writeLock().lock();
        try {
            if (tail == null)
                return null;
            T val = tail.elements[--tail.end];
            if (tail.start == tail.end)
                removeTail();
            size.decrementAndGet();
            return val;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ---------------- Bulk pop ----------------
    public List<T> popLeft(int count) {
        lock.writeLock().lock();
        try {
            if (count <= 0)
                return new ArrayList<>();
            if (count > size.get())
                throw new IndexOutOfBoundsException();

            List<T> result = new ArrayList<>(count);
            while (count > 0 && head != null) {
                int nodeSize = head.size();
                int take = Math.min(count, nodeSize);
                for (int i = 0; i < take; i++) {
                    result.add(head.elements[head.start++]);
                }
                if (head.start == head.end)
                    removeHead();
                count -= take;
            }
            size.addAndGet(-result.size());
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<T> popRight(int count) {
        lock.writeLock().lock();
        try {
            if (count <= 0)
                return new ArrayList<>();
            if (count > size.get())
                throw new IndexOutOfBoundsException();

            List<T> result = new ArrayList<>(count);
            while (count > 0 && tail != null) {
                int nodeSize = tail.size();
                int take = Math.min(count, nodeSize);
                for (int i = 0; i < take; i++) {
                    result.add(tail.elements[--tail.end]);
                }
                if (tail.start == tail.end)
                    removeTail();
                count -= take;
            }
            size.addAndGet(-result.size());
            // reverse to maintain order for left-to-right popping
            java.util.Collections.reverse(result);
            return result;
        } finally {
            lock.writeLock().unlock();
        }
    }


    public List<T> range(int start, int end) {
        lock.readLock().lock();
        try {
            List<T> result = new ArrayList<>();
            int totalSize = size.get();

            if (head == null || start < 0 || start > end || start >= totalSize) {
                return result;
            }

            // Clamp end within size
            if (end >= totalSize) {
                end = totalSize - 1;
            }

            int index = 0;
            Node<T> curr = head;

            while (curr != null && index <= end) {
                for (int i = curr.start; i < curr.end; i++) {
                    if (index >= start && index <= end) {
                        result.add(curr.elements[i]);
                    }
                    index++;
                    if (index > end)
                        break;
                }
                curr = curr.next;
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void removeHead() {
        head = head.next;
        if (head != null)
            head.prev = null;
        else
            tail = null;
    }

    private void removeTail() {
        tail = tail.prev;
        if (tail != null)
            tail.next = null;
        else
            head = null;
    }

    public int length() {
        return size.get();
    }
}
