package com.game421.hand;

/**
 * The six categories of a 421 hand.
 *
 * <p>Constants are declared from the weakest to the strongest category so that
 * {@link Enum#ordinal()} directly reflects their relative strength.
 */
public enum HandCategory {

    /** No special pattern; hands are compared die by die, highest first. */
    HIGH_DICE,

    /** Two dice of the same value; compared by pair value, then by the kicker. */
    ONE_PAIR,

    /** Three consecutive values (3-2-1 up to 6-5-4); compared by highest die. */
    STRAIGHT,

    /** Three dice of the same value, except aces; compared by value. */
    THREE_OF_A_KIND,

    /** The 1-1-1 combination, stronger than any other three of a kind. */
    TRIPLE_ACES,

    /** The eponymous 4-2-1 combination, the strongest hand in the game. */
    FOUR_TWO_ONE
}
