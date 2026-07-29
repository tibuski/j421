package com.game421.hand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HandEvaluatorTest {

    @Test
    void evaluates421InAnyOrder() {
        assertEquals(new Hand(HandCategory.FOUR_TWO_ONE, List.of()),
                HandEvaluator.evaluate(new int[]{2, 4, 1}));
    }

    @Test
    void evaluatesTripleAces() {
        assertEquals(new Hand(HandCategory.TRIPLE_ACES, List.of()),
                HandEvaluator.evaluate(new int[]{1, 1, 1}));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6})
    void evaluatesThreeOfAKind(int value) {
        assertEquals(new Hand(HandCategory.THREE_OF_A_KIND, List.of(value)),
                HandEvaluator.evaluate(new int[]{value, value, value}));
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6})
    void evaluatesStraight(int high) {
        assertEquals(new Hand(HandCategory.STRAIGHT, List.of(high)),
                HandEvaluator.evaluate(new int[]{high - 2, high, high - 1}));
    }

    @Test
    void evaluatesPairWithKicker() {
        assertEquals(new Hand(HandCategory.ONE_PAIR, List.of(5, 2)),
                HandEvaluator.evaluate(new int[]{5, 2, 5}));
    }

    @Test
    void evaluatesLowPairWithHighKicker() {
        assertEquals(new Hand(HandCategory.ONE_PAIR, List.of(3, 6)),
                HandEvaluator.evaluate(new int[]{3, 6, 3}));
    }

    @Test
    void evaluatesHighDiceDescending() {
        assertEquals(new Hand(HandCategory.HIGH_DICE, List.of(6, 3, 1)),
                HandEvaluator.evaluate(new int[]{3, 6, 1}));
    }

    @Test
    void rejectsWrongNumberOfDice() {
        assertThrows(IllegalArgumentException.class, () -> HandEvaluator.evaluate(new int[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> HandEvaluator.evaluate(new int[]{1, 2, 3, 4}));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, -1})
    void rejectsOutOfRangeValues(int value) {
        assertThrows(IllegalArgumentException.class, () -> HandEvaluator.evaluate(new int[]{1, 2, value}));
    }

    @Test
    void rejectsNullDice() {
        assertThrows(NullPointerException.class, () -> HandEvaluator.evaluate(null));
    }

    @Test
    void doesNotMutateInput() {
        int[] dice = {2, 4, 1};
        HandEvaluator.evaluate(dice);
        assertEquals(2, dice[0]);
        assertEquals(4, dice[1]);
        assertEquals(1, dice[2]);
    }
}
