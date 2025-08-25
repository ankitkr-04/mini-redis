package commands.impl.sortedsets;

/**
 * Configuration record for ZRANGE command arguments.
 * Helps avoid primitive obsession and makes the API clearer.
 */
public record ZRangeArgs(
        String key,
        int start,
        int end,
        boolean withScores,
        boolean byScore,
        boolean reverse) {

    public static ZRangeArgs byRank(String key, int start, int end) {
        return new ZRangeArgs(key, start, end, false, false, false);
    }

    public static ZRangeArgs byRankWithScores(String key, int start, int end) {
        return new ZRangeArgs(key, start, end, true, false, false);
    }

    public static ZRangeArgs byScore(String key, int start, int end) {
        return new ZRangeArgs(key, start, end, false, true, false);
    }

    public static ZRangeArgs byScoreWithScores(String key, int start, int end) {
        return new ZRangeArgs(key, start, end, true, true, false);
    }

    public ZRangeArgs withScoresEnabled() {
        return new ZRangeArgs(key, start, end, true, byScore, reverse);
    }

    public ZRangeArgs reversed() {
        return new ZRangeArgs(key, start, end, withScores, byScore, true);
    }
}
