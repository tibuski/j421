package com.game421.player;

import com.game421.turn.Turn;
import com.game421.turn.TurnDecision;
import com.game421.turn.TurnState;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AiPlayerControllerTest {

    private final AiPlayerController ai = new AiPlayerController();

    private static TurnState state(int[] dice, int rollsRemaining) {
        return new TurnState(dice, Turn.MAX_ROLLS - rollsRemaining, rollsRemaining);
    }

    @Test
    void standsWith421() {
        TurnDecision decision = ai.decide(state(new int[]{4, 2, 1}, 2));
        assertInstanceOf(TurnDecision.Stand.class, decision);
    }

    @Test
    void standsWithTripleAces() {
        TurnDecision decision = ai.decide(state(new int[]{1, 1, 1}, 2));
        assertInstanceOf(TurnDecision.Stand.class, decision);
    }

    @Test
    void standsWithThreeOfAKind() {
        TurnDecision decision = ai.decide(state(new int[]{3, 3, 3}, 2));
        assertInstanceOf(TurnDecision.Stand.class, decision);
    }

    @Test
    void standsWithStraight() {
        TurnDecision decision = ai.decide(state(new int[]{5, 4, 3}, 2));
        assertInstanceOf(TurnDecision.Stand.class, decision);
    }

    @Test
    void rerollsOnlyTheKickerWithAPair() {
        TurnDecision decision = ai.decide(state(new int[]{5, 2, 5}, 2));
        assertEquals(new TurnDecision.Reroll(Set.of(1)), decision);
    }

    @Test
    void keeps421BuildingBlocksOtherwise() {
        TurnDecision decision = ai.decide(state(new int[]{4, 6, 1}, 2));
        assertEquals(new TurnDecision.Reroll(Set.of(1)), decision);
    }

    @Test
    void keepsHighestDieWhenNothingUseful() {
        TurnDecision decision = ai.decide(state(new int[]{6, 3, 5}, 2));
        assertEquals(new TurnDecision.Reroll(Set.of(1, 2)), decision);
    }

    @Test
    void standsWhenNoRollsRemain() {
        TurnDecision decision = ai.decide(state(new int[]{6, 3, 5}, 0));
        assertInstanceOf(TurnDecision.Stand.class, decision);
    }
}
