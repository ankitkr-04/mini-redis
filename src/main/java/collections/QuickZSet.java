package collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public final class QuickZSet {
    private final ConcurrentSkipListMap<Double, ConcurrentSkipListSet<String>> scoreMap = new ConcurrentSkipListMap<>();
    private final ConcurrentHashMap<String, Double> memberMap = new ConcurrentHashMap<>();

    /** Add or update a member, returns true if new member was added */
    public boolean add(String member, double score) {
        Double oldScore = memberMap.put(member, score);
        if (oldScore != null) {
            scoreMap.computeIfPresent(oldScore, (k, set) -> {
                set.remove(member);
                return set.isEmpty() ? null : set;
            });
        }
        scoreMap.compute(score, (k, set) -> {
            if (set == null)
                set = new ConcurrentSkipListSet<>();
            set.add(member);
            return set;
        });
        return oldScore == null; // Return true if this was a new member
    }

    /** Remove a member */
    public boolean remove(String member) {
        Double score = memberMap.remove(member);
        if (score == null)
            return false;
        scoreMap.computeIfPresent(score, (k, set) -> {
            set.remove(member);
            return set.isEmpty() ? null : set;
        });
        return true;
    }

    /** Pop member with smallest score */
    public Optional<ZSetEntry> popMin() {
        while (!scoreMap.isEmpty()) {
            Map.Entry<Double, ConcurrentSkipListSet<String>> entry = scoreMap.firstEntry();
            if (entry == null)
                return Optional.empty();
            String member = entry.getValue().pollFirst();
            if (member != null) {
                memberMap.remove(member);
                if (entry.getValue().isEmpty())
                    scoreMap.remove(entry.getKey(), entry.getValue());
                return Optional.of(new ZSetEntry(member, entry.getKey()));
            }
        }
        return Optional.empty();
    }

    /** Pop member with largest score */
    public Optional<ZSetEntry> popMax() {
        while (!scoreMap.isEmpty()) {
            Map.Entry<Double, ConcurrentSkipListSet<String>> entry = scoreMap.lastEntry();
            if (entry == null)
                return Optional.empty();
            String member = entry.getValue().pollLast();
            if (member != null) {
                memberMap.remove(member);
                if (entry.getValue().isEmpty())
                    scoreMap.remove(entry.getKey(), entry.getValue());
                return Optional.of(new ZSetEntry(member, entry.getKey()));
            }
        }
        return Optional.empty();
    }

    /** Range by index, supports negative indices */
    public List<ZSetEntry> range(int start, int end) {
        int size = memberMap.size();
        if (start < 0)
            start += size;
        if (end < 0)
            end += size;
        start = Math.max(0, start);
        end = Math.min(size - 1, end);
        if (start > end)
            return List.of();

        List<ZSetEntry> result = new ArrayList<>(end - start + 1);
        int idx = 0;
        for (var entry : scoreMap.entrySet()) {
            for (String member : entry.getValue()) {
                if (idx > end)
                    return result;
                if (idx >= start)
                    result.add(new ZSetEntry(member, entry.getKey()));
                idx++;
            }
        }
        return result;
    }

    /** Range by score inclusive */
    public List<ZSetEntry> rangeByScore(double min, double max) {
        List<ZSetEntry> result = new ArrayList<>();
        scoreMap.subMap(min, true, max, true).forEach((score, set) -> {
            for (String member : set)
                result.add(new ZSetEntry(member, score));
        });
        return result;
    }

    /** Check if member exists */
    public boolean contains(String member) {
        return memberMap.containsKey(member);
    }

    /** Get size */
    public int size() {
        return memberMap.size();
    }

    /** Get score for a member */
    public Double getScore(String member) {
        return memberMap.get(member);
    }

    /** Get rank (0-based index) for a member */
    public Long getRank(String member) {
        Double memberScore = memberMap.get(member);
        if (memberScore == null) {
            return null;
        }

        long rank = 0;
        for (var entry : scoreMap.entrySet()) {
            if (entry.getKey().equals(memberScore)) {
                // Found the score bucket, find position within it
                for (String m : entry.getValue()) {
                    if (m.equals(member)) {
                        return rank;
                    }
                    rank++;
                }
            } else if (entry.getKey() < memberScore) {
                // All members in this bucket have lower scores
                rank += entry.getValue().size();
            } else {
                // Higher score bucket, member not found
                break;
            }
        }
        return null;
    }

    public record ZSetEntry(String member, double score) {
    }
}
