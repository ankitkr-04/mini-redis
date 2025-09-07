package collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe implementation of a Redis-like sorted set (ZSet).
 * Supports add, remove, pop min/max, range queries, and rank retrieval.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public final class QuickZSet {

    /** Logger for QuickZSet */
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickZSet.class);

    private final ConcurrentSkipListMap<Double, ConcurrentSkipListSet<String>> scoreMap = new ConcurrentSkipListMap<>();
    private final ConcurrentHashMap<String, Double> memberMap = new ConcurrentHashMap<>();

    /**
     * Adds or updates a member with the given score.
     * 
     * @param member the member to add or update
     * @param score  the score associated with the member
     * @return true if a new member was added, false if updated
     */
    public boolean add(String member, double score) {
        Double oldScore = memberMap.put(member, score);
        if (oldScore != null) {
            scoreMap.computeIfPresent(oldScore, (k, set) -> {
                set.remove(member);
                return set.isEmpty() ? null : set;
            });
            LOGGER.debug("Updated member '{}' from score {} to {}", member, oldScore, score);
        } else {
            LOGGER.debug("Added new member '{}' with score {}", member, score);
        }
        scoreMap.compute(score, (k, set) -> {
            if (set == null)
                set = new ConcurrentSkipListSet<>();
            set.add(member);
            return set;
        });
        return oldScore == null;
    }

    /**
     * Removes a member from the set.
     * 
     * @param member the member to remove
     * @return true if the member was removed, false if not found
     */
    public boolean remove(String member) {
        Double score = memberMap.remove(member);
        if (score == null)
            return false;
        scoreMap.computeIfPresent(score, (k, set) -> {
            set.remove(member);
            return set.isEmpty() ? null : set;
        });
        LOGGER.debug("Removed member '{}' with score {}", member, score);
        return true;
    }

    /**
     * Pops and returns the member with the smallest score.
     * 
     * @return Optional containing the entry, or empty if set is empty
     */
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
                LOGGER.debug("Popped min member '{}' with score {}", member, entry.getKey());
                return Optional.of(new ZSetEntry(member, entry.getKey()));
            }
        }
        return Optional.empty();
    }

    /**
     * Pops and returns the member with the largest score.
     * 
     * @return Optional containing the entry, or empty if set is empty
     */
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
                LOGGER.debug("Popped max member '{}' with score {}", member, entry.getKey());
                return Optional.of(new ZSetEntry(member, entry.getKey()));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a range of entries by index with early termination.
     * 
     * @param start start index (inclusive), negative for offset from end
     * @param end   end index (inclusive), negative for offset from end
     * @return list of entries in the specified range
     */
    public List<ZSetEntry> range(int start, int end) {
        final int size = memberMap.size();
        if (size == 0) {
            return List.of();
        }
        
        // Normalize negative indices
        if (start < 0) start += size;
        if (end < 0) end += size;
        
        // Bounds validation with early exit
        start = Math.max(0, start);
        end = Math.min(size - 1, end);
        if (start > end || start >= size) {
            return List.of();
        }

        // Pre-size the result list for better memory allocation
        final List<ZSetEntry> result = new ArrayList<>(end - start + 1);
        int currentIndex = 0;
        
        // Iteration with early termination
        for (var scoreEntry : scoreMap.entrySet()) {
            final double score = scoreEntry.getKey();
            final ConcurrentSkipListSet<String> members = scoreEntry.getValue();
            
            for (String member : members) {
                // Early termination - we've passed the end index
                if (currentIndex > end) {
                    return result;
                }
                
                // Add to result if within range
                if (currentIndex >= start) {
                    result.add(new ZSetEntry(member, score));
                }
                currentIndex++;
            }
        }
        
        return result;
    }

    /**
     * Returns a range of entries with scores between min and max (inclusive).
     * 
     * @param min minimum score (inclusive)
     * @param max maximum score (inclusive)
     * @return list of entries in the specified score range
     */
    public List<ZSetEntry> rangeByScore(double min, double max) {
        List<ZSetEntry> result = new ArrayList<>();
        scoreMap.subMap(min, true, max, true).forEach((score, set) -> {
            for (String member : set)
                result.add(new ZSetEntry(member, score));
        });
        return result;
    }

    /**
     * Checks if a member exists in the set.
     * 
     * @param member the member to check
     * @return true if the member exists, false otherwise
     */
    public boolean contains(String member) {
        return memberMap.containsKey(member);
    }

    /**
     * Returns the number of members in the set.
     * 
     * @return size of the set
     */
    public int size() {
        return memberMap.size();
    }

    /**
     * Gets the score for a given member.
     * 
     * @param member the member to query
     * @return the score, or null if not found
     */
    public Double getScore(String member) {
        return memberMap.get(member);
    }

    /**
     * Gets the rank (0-based index) for a member.
     * 
     * @param member the member to query
     * @return rank if found, null otherwise
     */
    public Long getRank(String member) {
        Double memberScore = memberMap.get(member);
        if (memberScore == null) {
            return null;
        }

        long rank = 0;
        
        // Rank calculation with early termination
        for (var entry : scoreMap.entrySet()) {
            final double currentScore = entry.getKey();
            
            if (currentScore > memberScore) {
                // Early termination - scores are sorted, so we won't find our member
                break;
            }
            
            if (currentScore == memberScore) {
                // Found the score bucket, search within it
                for (String m : entry.getValue()) {
                    if (m.equals(member)) {
                        return rank;
                    }
                    rank++;
                }
            } else {
                // Add all members from lower score buckets
                rank += entry.getValue().size();
            }
        }
        
        return null;
    }

    /**
     * Immutable entry representing a member and its score.
     * 
     * @param member the member name
     * @param score  the score value
     */
    public record ZSetEntry(String member, double score) {
    }
}
