package com.game421.hand;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates a set of three dice into a ranked {@link Hand}.
 */
public final class HandEvaluator {

    /** Number of dice making up a hand. */
    public static final int HAND_SIZE = 3;

    private static final int MIN_DIE_VALUE = 1;
    private static final int MAX_DIE_VALUE = 6;

    private HandEvaluator() {
        // Utility class: not instantiable.
    }

    /**
     * Evaluates the given dice into a {@link Hand}.
     *
     * @param dice exactly {@value #HAND_SIZE} die values, each between 1 and 6
     * @return the evaluated hand
     * @throws IllegalArgumentException if the number of dice is wrong or a value is out of range
     */
    public static Hand evaluate(int[] dice) {
        Objects.requireNonNull(dice, "dice must not be null");
        if (dice.length != HAND_SIZE) {
            throw new IllegalArgumentException(
                    "A hand requires exactly " + HAND_SIZE + " dice, got " + dice.length);
        }
        for (int value : dice) {
            if (value < MIN_DIE_VALUE || value > MAX_DIE_VALUE) {
                throw new IllegalArgumentException(
                        "Die values must be between " + MIN_DIE_VALUE + " and " + MAX_DIE_VALUE + ", got " + value);
            }
        }

        int[] sorted = dice.clone();
        Arrays.sort(sorted);
        int low = sorted[0];
        int mid = sorted[1];
        int high = sorted[2];

        if (low == 1 && mid == 2 && high == 4) {
            return new Hand(HandCategory.FOUR_TWO_ONE, List.of());
        }
        if (low == mid && mid == high) {
            return low == 1
                    ? new Hand(HandCategory.TRIPLE_ACES, List.of())
                    : new Hand(HandCategory.THREE_OF_A_KIND, List.of(high));
        }
        if (mid - low == 1 && high - mid == 1) {
            return new Hand(HandCategory.STRAIGHT, List.of(high));
        }
        if (mid == high) {
            return new Hand(HandCategory.ONE_PAIR, List.of(mid, low));
        }
        if (low == mid) {
            return new Hand(HandCategory.ONE_PAIR, List.of(low, high));
        }
        return new Hand(HandCategory.HIGH_DICE, List.of(high, mid, low));
    }
}
