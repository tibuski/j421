package com.game421.hand;

import java.util.List;
import java.util.Objects;

/**
 * An evaluated, immutable 421 hand.
 *
 * <p>A hand is defined by its {@link HandCategory} and a list of tie-breaker
 * values used to rank hands of the same category (e.g. the pair value and the
 * kicker for {@link HandCategory#ONE_PAIR}). Hands are naturally ordered from
 * weakest to strongest via {@link #compareTo(Hand)}.
 *
 * @param category    the category of the hand
 * @param tieBreakers the values used to compare hands of the same category,
 *                    ordered from most to least significant
 */
public record Hand(HandCategory category, List<Integer> tieBreakers) implements Comparable<Hand> {

    public Hand {
        Objects.requireNonNull(category, "category must not be null");
        tieBreakers = List.copyOf(tieBreakers);
    }

    /**
     * Compares this hand to another, strongest first: by category, then by
     * tie-breaker values in order of significance.
     */
    @Override
    public int compareTo(Hand other) {
        int byCategory = Integer.compare(category.ordinal(), other.category.ordinal());
        if (byCategory != 0) {
            return byCategory;
        }
        int common = Math.min(tieBreakers.size(), other.tieBreakers.size());
        for (int i = 0; i < common; i++) {
            int byValue = Integer.compare(tieBreakers.get(i), other.tieBreakers.get(i));
            if (byValue != 0) {
                return byValue;
            }
        }
        return Integer.compare(tieBreakers.size(), other.tieBreakers.size());
    }

    /**
     * Returns a human-readable description of this hand,
     * e.g. {@code "Pair of 4s with kicker 2"}.
     *
     * @return a textual description of the hand
     */
    public String describe() {
        return switch (category) {
            case FOUR_TWO_ONE -> "421";
            case TRIPLE_ACES -> "Triple aces (1-1-1)";
            case THREE_OF_A_KIND -> {
                int value = tieBreakers.getFirst();
                yield "Three of a kind (%d-%d-%d)".formatted(value, value, value);
            }
            case STRAIGHT -> {
                int high = tieBreakers.getFirst();
                yield "Straight (%d-%d-%d)".formatted(high, high - 1, high - 2);
            }
            case ONE_PAIR -> "Pair of %ds with kicker %d".formatted(tieBreakers.get(0), tieBreakers.get(1));
            case HIGH_DICE -> "High dice (%d-%d-%d)".formatted(tieBreakers.get(0), tieBreakers.get(1), tieBreakers.get(2));
        };
    }
}
