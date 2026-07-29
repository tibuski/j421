package com.game421.turn;

import com.game421.dice.DiceRoller;
import com.game421.hand.Hand;
import com.game421.hand.HandEvaluator;

import java.util.Objects;
import java.util.Set;

/**
 * A single player's turn: up to {@value #MAX_ROLLS} rolls of
 * {@value #DICE_COUNT} dice. After the initial roll of all dice, the player
 * may reroll any subset of dice or stand with the current hand.
 *
 * <p>Not thread-safe.
 */
public final class Turn {

    /** Number of dice rolled during a turn. */
    public static final int DICE_COUNT = 3;

    /** Maximum number of rolls allowed per turn, including the initial one. */
    public static final int MAX_ROLLS = 3;

    private final DiceRoller roller;
    private final int[] dice = new int[DICE_COUNT];

    private int rollsUsed;
    private boolean started;
    private boolean finished;

    /**
     * Creates a turn drawing die values from the given roller.
     *
     * @param roller the source of die values
     */
    public Turn(DiceRoller roller) {
        this.roller = Objects.requireNonNull(roller, "roller must not be null");
    }

    /**
     * Performs the initial roll of all dice.
     *
     * @throws IllegalStateException if the turn was already started
     */
    public void start() {
        if (started) {
            throw new IllegalStateException("Turn has already started");
        }
        started = true;
        for (int i = 0; i < DICE_COUNT; i++) {
            dice[i] = roller.roll();
        }
        rollsUsed = 1;
    }

    /**
     * Rerolls the dice at the given positions, keeping all others.
     *
     * @param positions 0-based die positions to reroll; must not be empty
     * @throws IllegalStateException    if the turn has not started or is already finished
     * @throws IllegalArgumentException if a position is out of range or the set is empty
     */
    public void reroll(Set<Integer> positions) {
        ensureStarted();
        ensureNotFinished();
        validatePositions(positions);
        for (int position : positions) {
            dice[position] = roller.roll();
        }
        rollsUsed++;
        if (rollsUsed >= MAX_ROLLS) {
            finished = true;
        }
    }

    /**
     * Ends the turn, keeping the current dice.
     *
     * @throws IllegalStateException if the turn has not started
     */
    public void stand() {
        ensureStarted();
        finished = true;
    }

    /**
     * Evaluates the current dice into a {@link Hand}.
     *
     * @return the current hand
     * @throws IllegalStateException if the turn has not started
     */
    public Hand result() {
        ensureStarted();
        return HandEvaluator.evaluate(dice);
    }

    /**
     * Returns an immutable snapshot of the current turn state.
     *
     * @return the current state
     */
    public TurnState snapshot() {
        return new TurnState(dice, rollsUsed, rollsRemaining());
    }

    public boolean isFinished() {
        return finished;
    }

    public int rollsUsed() {
        return rollsUsed;
    }

    public int rollsRemaining() {
        return MAX_ROLLS - rollsUsed;
    }

    /**
     * Returns a copy of the current die values, in position order.
     *
     * @return the current dice
     */
    public int[] dice() {
        return dice.clone();
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("Turn has not started yet");
        }
    }

    private void ensureNotFinished() {
        if (finished) {
            throw new IllegalStateException("Turn is already finished");
        }
    }

    private static void validatePositions(Set<Integer> positions) {
        if (positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("At least one die position is required to reroll");
        }
        for (int position : positions) {
            if (position < 0 || position >= DICE_COUNT) {
                throw new IllegalArgumentException(
                        "Die position must be between 0 and " + (DICE_COUNT - 1) + ", got " + position);
            }
        }
    }
}
