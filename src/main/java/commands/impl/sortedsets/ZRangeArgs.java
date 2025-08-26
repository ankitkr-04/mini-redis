package commands.impl.sortedsets;

/**
 * Represents arguments for the ZRANGE command in a sorted set.
 * Encapsulates all possible options for ZRANGE, including range type and score
 * inclusion.
 *
 * @author Ankit Kumar
 * @version 1.0
 */
public record ZRangeArgs(
        String key,
        int startIndex,
        int endIndex,
        boolean includeScores,
        boolean rangeByScore,
        boolean isReversed) {

    // Default values for argument flags
    private static final boolean SCORES_DISABLED = false;
    private static final boolean SCORES_ENABLED = true;
    private static final boolean BY_SCORE_DISABLED = false;
    private static final boolean BY_SCORE_ENABLED = true;
    private static final boolean REVERSED_DISABLED = false;
    private static final boolean REVERSED_ENABLED = true;

    /**
     * Creates ZRangeArgs for a rank-based range without scores.
     *
     * @param key        the sorted set key
     * @param startIndex start index
     * @param endIndex   end index
     * @return ZRangeArgs instance
     */
    public static ZRangeArgs byRank(String key, int startIndex, int endIndex) {
        return new ZRangeArgs(key, startIndex, endIndex, SCORES_DISABLED, BY_SCORE_DISABLED, REVERSED_DISABLED);
    }

    /**
     * Creates ZRangeArgs for a rank-based range with scores.
     *
     * @param key        the sorted set key
     * @param startIndex start index
     * @param endIndex   end index
     * @return ZRangeArgs instance
     */
    public static ZRangeArgs byRankWithScores(String key, int startIndex, int endIndex) {
        return new ZRangeArgs(key, startIndex, endIndex, SCORES_ENABLED, BY_SCORE_DISABLED, REVERSED_DISABLED);
    }

    /**
     * Creates ZRangeArgs for a score-based range without scores.
     *
     * @param key        the sorted set key
     * @param startIndex start score (inclusive)
     * @param endIndex   end score (inclusive)
     * @return ZRangeArgs instance
     */
    public static ZRangeArgs byScore(String key, int startIndex, int endIndex) {
        return new ZRangeArgs(key, startIndex, endIndex, SCORES_DISABLED, BY_SCORE_ENABLED, REVERSED_DISABLED);
    }

    /**
     * Creates ZRangeArgs for a score-based range with scores.
     *
     * @param key        the sorted set key
     * @param startIndex start score (inclusive)
     * @param endIndex   end score (inclusive)
     * @return ZRangeArgs instance
     */
    public static ZRangeArgs byScoreWithScores(String key, int startIndex, int endIndex) {
        return new ZRangeArgs(key, startIndex, endIndex, SCORES_ENABLED, BY_SCORE_ENABLED, REVERSED_DISABLED);
    }

    /**
     * Returns a new ZRangeArgs instance with scores enabled.
     *
     * @return ZRangeArgs with includeScores set to true
     */
    public ZRangeArgs withScoresEnabled() {
        return new ZRangeArgs(key, startIndex, endIndex, SCORES_ENABLED, rangeByScore, isReversed);
    }

    /**
     * Returns a new ZRangeArgs instance with reversed order enabled.
     *
     * @return ZRangeArgs with isReversed set to true
     */
    public ZRangeArgs reversed() {
        return new ZRangeArgs(key, startIndex, endIndex, includeScores, rangeByScore, REVERSED_ENABLED);
    }
}
