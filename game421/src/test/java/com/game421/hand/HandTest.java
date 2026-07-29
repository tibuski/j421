package com.game421.hand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandTest {

    private static Hand fourTwoOne() {
        return new Hand(HandCategory.FOUR_TWO_ONE, List.of());
    }

    private static Hand tripleAces() {
        return new Hand(HandCategory.TRIPLE_ACES, List.of());
    }

    private static Hand threeOfAKind(int value) {
        return new Hand(HandCategory.THREE_OF_A_KIND, List.of(value));
    }

    private static Hand straight(int high) {
        return new Hand(HandCategory.STRAIGHT, List.of(high));
    }

    private static Hand pair(int value, int kicker) {
        return new Hand(HandCategory.ONE_PAIR, List.of(value, kicker));
    }

    private static Hand highDice(int a, int b, int c) {
        return new Hand(HandCategory.HIGH_DICE, List.of(a, b, c));
    }

    /** Pairs of hands ordered from strictly weaker to strictly stronger. */
    static Stream<Hand[]> orderedPairs() {
        return Stream.of(
                new Hand[]{highDice(6, 5, 2), pair(2, 6)},
                new Hand[]{pair(2, 6), straight(3)},
                new Hand[]{straight(6), threeOfAKind(2)},
                new Hand[]{threeOfAKind(6), tripleAces()},
                new Hand[]{tripleAces(), fourTwoOne()},
                // Tie-breakers within a category.
                new Hand[]{threeOfAKind(4), threeOfAKind(5)},
                new Hand[]{straight(4), straight(5)},
                new Hand[]{pair(3, 6), pair(4, 2)},
                new Hand[]{pair(4, 2), pair(4, 5)},
                new Hand[]{highDice(6, 4, 6), highDice(6, 5, 1)}
        );
    }

    @ParameterizedTest
    @MethodSource("orderedPairs")
    void weakerHandComparesBelowStrongerHand(Hand weaker, Hand stronger) {
        assertTrue(weaker.compareTo(stronger) < 0, weaker + " should be weaker than " + stronger);
        assertTrue(stronger.compareTo(weaker) > 0, stronger + " should be stronger than " + weaker);
    }

    @Test
    void equalHandsCompareToZero() {
        assertEquals(0, pair(4, 5).compareTo(pair(4, 5)));
        assertEquals(pair(4, 5), pair(4, 5));
    }

    @Test
    void comparisonIsConsistentWithEquality() {
        for (Hand[] pair : orderedPairs().toList()) {
            assertTrue(pair[0].compareTo(pair[1]) != 0);
            assertTrue(!pair[0].equals(pair[1]));
        }
    }

    @Test
    void describesHands() {
        assertEquals("421", fourTwoOne().describe());
        assertEquals("Triple aces (1-1-1)", tripleAces().describe());
        assertEquals("Three of a kind (6-6-6)", threeOfAKind(6).describe());
        assertEquals("Straight (5-4-3)", straight(5).describe());
        assertEquals("Pair of 4s with kicker 2", pair(4, 2).describe());
        assertEquals("High dice (6-3-1)", highDice(6, 3, 1).describe());
    }
}
