package com.game421.player;

import com.game421.turn.TurnDecision;
import com.game421.turn.TurnState;

/**
 * Strategy deciding how a player acts during their turn. Implementations may
 * ask a human on the console, apply an AI heuristic, or replay scripted
 * decisions in tests.
 */
@FunctionalInterface
public interface PlayerController {

    /**
     * Decides the next move given the current turn state.
     *
     * @param state an immutable snapshot of the ongoing turn
     * @return the decision to apply; must not be {@code null}
     */
    TurnDecision decide(TurnState state);
}
