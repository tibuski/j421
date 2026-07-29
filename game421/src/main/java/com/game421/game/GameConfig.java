package com.game421.game;

/**
 * Settings of a match.
 *
 * @param winningScore the number of points required to win the match
 */
public record GameConfig(int winningScore) {

    /** Default number of points required to win a match. */
    public static final int DEFAULT_WINNING_SCORE = 5;

    public GameConfig {
        if (winningScore < 1) {
            throw new IllegalArgumentException("Winning score must be at least 1, got " + winningScore);
        }
    }

    /**
     * Returns the standard configuration.
     *
     * @return a configuration with the default winning score
     */
    public static GameConfig standard() {
        return new GameConfig(DEFAULT_WINNING_SCORE);
    }
}
