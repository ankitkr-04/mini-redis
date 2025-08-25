package collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.locks.StampedLock;

/**
 * QuickZSet: Redis-like Sorted Set implementation
 * Supports:
 * - O(log N) insert/remove using skip list ordering
 * - Range queries by index with negative indexes
 * - Pop min/max
 * - Membership lookup in O(1)
 */
public final class QuickZSet {
    public static final class ZSetEntry implements Comparable<ZSetEntry> {
        public final String member;
        public final double score;

        public ZSetEntry(String member, double score) {
            this.member = member;
            this.score = score;
        }

        @Override
        public int compareTo(ZSetEntry o) {
            int cmp = Double.compare(this.score, o.score);
            return cmp != 0 ? cmp : this.member.compareTo(o.member);
        }

        @Override
        public String toString() {
            return member + "(" + score + ")";
        }
    }

    private final StampedLock lock = new StampedLock();
    private final Map<String, ZSetEntry> memberMap = new HashMap<>();
    private final TreeSet<ZSetEntry> orderedSet = new TreeSet<>();

    /** Add or update a member with a score */
    public void add(String member, double score) {
        long stamp = lock.writeLock();
        try {
            ZSetEntry old = memberMap.get(member);
            if (old != null) {
                orderedSet.remove(old);
            }
            ZSetEntry entry = new ZSetEntry(member, score);
            orderedSet.add(entry);
            memberMap.put(member, entry);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /** Remove a member */
    public boolean remove(String member) {
        long stamp = lock.writeLock();
        try {
            ZSetEntry old = memberMap.remove(member);
            if (old != null) {
                orderedSet.remove(old);
                return true;
            }
            return false;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /** Pop the member with the smallest score */
    public ZSetEntry popMin() {
        long stamp = lock.writeLock();
        try {
            ZSetEntry first = orderedSet.pollFirst();
            if (first != null)
                memberMap.remove(first.member);
            return first;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /** Pop the member with the largest score */
    public ZSetEntry popMax() {
        long stamp = lock.writeLock();
        try {
            ZSetEntry last = orderedSet.pollLast();
            if (last != null)
                memberMap.remove(last.member);
            return last;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /** Get size */
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int sz = memberMap.size();
        return lock.validate(stamp) ? sz : memberMap.size();
    }

    /** Get range by rank (supports negative indexes) */
    public List<ZSetEntry> range(int start, int end) {
        long stamp = lock.readLock();
        try {
            if (start < 0)
                start = size() + start;
            if (end < 0)
                end = size() + end;
            start = Math.max(0, start);
            end = Math.min(size() - 1, end);
            if (start > end)
                return List.of();

            List<ZSetEntry> result = new ArrayList<>(end - start + 1);
            int idx = 0;
            for (ZSetEntry e : orderedSet) {
                if (idx > end)
                    break;
                if (idx >= start)
                    result.add(e);
                idx++;
            }
            return result;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /** Get range by score (inclusive) */
    public List<ZSetEntry> rangeByScore(double min, double max) {
        long stamp = lock.readLock();
        try {
            ZSetEntry low = new ZSetEntry("", min);
            ZSetEntry high = new ZSetEntry("", max);
            return new ArrayList<>(orderedSet.subSet(low, true, high, true));
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /** Check if member exists */
    public boolean contains(String member) {
        long stamp = lock.tryOptimisticRead();
        boolean exists = memberMap.containsKey(member);
        return lock.validate(stamp) ? exists : memberMap.containsKey(member);
    }

    @Override
    public String toString() {
        long stamp = lock.readLock();
        try {
            return "QuickZSet" + orderedSet.toString();
        } finally {
            lock.unlockRead(stamp);
        }
    }
}
