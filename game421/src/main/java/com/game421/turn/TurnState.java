package com.game421.turn;

/**
 * Immutable snapshot of an ongoing turn, handed to players so they can decide
 * their next move without being able to mutate the turn itself.
 *
 * @param dice           the current die values, in position order
 * @param rollsUsed      the number of rolls performed so far (at least 1)
 * @param rollsRemaining the number of rolls still available
 */
public record TurnState(int[] dice, int rollsUsed, int rollsRemaining) {

    public TurnState {
        dice = dice.clone();
    }

    @Override
    public int[] dice() {
        return dice.clone();
    }
}
