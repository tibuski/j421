package com.game421.turn;

import java.util.Set;

/**
 * A decision made by a player during their turn: either keep the current
 * dice ({@link Stand}) or reroll some of them ({@link Reroll}).
 */
public sealed interface TurnDecision {

    /** Keep the current dice and end the turn. */
    record Stand() implements TurnDecision {
    }

    /**
     * Reroll the dice at the given positions, keeping the others.
     *
     * @param positions 0-based die positions to reroll; must not be empty
     */
    record Reroll(Set<Integer> positions) implements TurnDecision {

        public Reroll {
            positions = Set.copyOf(positions);
            if (positions.isEmpty()) {
                throw new IllegalArgumentException(
                        "Rerolling requires at least one die position; use Stand to keep all dice");
            }
        }
    }
}
