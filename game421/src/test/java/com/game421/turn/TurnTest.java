package com.game421.turn;

import com.game421.dice.DiceRoller;
import com.game421.hand.HandCategory;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnTest {

    /** Returns a roller yielding the scripted values in order. */
    static DiceRoller scriptedRoller(int... values) {
        Queue<Integer> queue = new ArrayDeque<>();
        for (int value : values) {
            queue.add(value);
        }
        return () -> {
            if (queue.isEmpty()) {
                throw new AssertionError("Scripted roller ran out of values");
            }
            return queue.remove();
        };
    }

    @Test
    void startRollsAllDice() {
        Turn turn = new Turn(scriptedRoller(6, 4, 2));
        turn.start();

        assertArrayEquals(new int[]{6, 4, 2}, turn.dice());
        assertEquals(1, turn.rollsUsed());
        assertEquals(2, turn.rollsRemaining());
        assertFalse(turn.isFinished());
    }

    @Test
    void rerollOnlyChangesSelectedPositions() {
        Turn turn = new Turn(scriptedRoller(6, 6, 6, 4));
        turn.start();
        turn.reroll(Set.of(1));

        assertArrayEquals(new int[]{6, 4, 6}, turn.dice());
        assertEquals(2, turn.rollsUsed());
    }

    @Test
    void rerollMayTargetAllPositions() {
        Turn turn = new Turn(scriptedRoller(1, 1, 1, 4, 2, 1));
        turn.start();
        // LinkedHashSet: positions are rerolled in the order 0, 1, 2.
        turn.reroll(new LinkedHashSet<>(List.of(0, 1, 2)));

        assertArrayEquals(new int[]{4, 2, 1}, turn.dice());
    }

    @Test
    void turnEndsAfterThirdRoll() {
        Turn turn = new Turn(scriptedRoller(1, 2, 3, 4, 5));
        turn.start();
        turn.reroll(Set.of(0));
        turn.reroll(Set.of(1));

        assertTrue(turn.isFinished());
        assertEquals(3, turn.rollsUsed());
        assertEquals(0, turn.rollsRemaining());
    }

    @Test
    void standEndsTurnEarly() {
        Turn turn = new Turn(scriptedRoller(4, 2, 1));
        turn.start();
        turn.stand();

        assertTrue(turn.isFinished());
        assertEquals(1, turn.rollsUsed());
    }

    @Test
    void rerollAfterFinishIsRejected() {
        Turn turn = new Turn(scriptedRoller(4, 2, 1));
        turn.start();
        turn.stand();

        assertThrows(IllegalStateException.class, () -> turn.reroll(Set.of(0)));
    }

    @Test
    void actionsBeforeStartAreRejected() {
        Turn turn = new Turn(scriptedRoller(1, 2, 3));

        assertThrows(IllegalStateException.class, () -> turn.reroll(Set.of(0)));
        assertThrows(IllegalStateException.class, turn::stand);
        assertThrows(IllegalStateException.class, turn::result);
    }

    @Test
    void startTwiceIsRejected() {
        Turn turn = new Turn(scriptedRoller(1, 2, 3));
        turn.start();

        assertThrows(IllegalStateException.class, turn::start);
    }

    @Test
    void invalidRerollPositionsAreRejected() {
        Turn turn = new Turn(scriptedRoller(1, 2, 3, 4));
        turn.start();

        assertThrows(IllegalArgumentException.class, () -> turn.reroll(Set.of()));
        assertThrows(IllegalArgumentException.class, () -> turn.reroll(Set.of(-1)));
        assertThrows(IllegalArgumentException.class, () -> turn.reroll(Set.of(Turn.DICE_COUNT)));
    }

    @Test
    void resultEvaluatesCurrentHand() {
        Turn turn = new Turn(scriptedRoller(1, 2, 4));
        turn.start();

        assertEquals(HandCategory.FOUR_TWO_ONE, turn.result().category());
    }

    @Test
    void snapshotExposesStateWithoutAllowingMutation() {
        Turn turn = new Turn(scriptedRoller(6, 4, 2));
        turn.start();

        TurnState snapshot = turn.snapshot();
        assertArrayEquals(new int[]{6, 4, 2}, snapshot.dice());
        assertEquals(1, snapshot.rollsUsed());
        assertEquals(2, snapshot.rollsRemaining());

        snapshot.dice()[0] = 1;
        assertArrayEquals(new int[]{6, 4, 2}, turn.dice());
    }
}
