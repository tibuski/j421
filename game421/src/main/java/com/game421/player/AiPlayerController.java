package com.game421.player;

import com.game421.hand.Hand;
import com.game421.hand.HandEvaluator;
import com.game421.turn.TurnDecision;
import com.game421.turn.TurnState;

import java.util.HashSet;
import java.util.Set;

/**
 * Computer opponent applying a simple heuristic:
 *
 * <ul>
 *   <li>stand with a straight or better (including 421);</li>
 *   <li>with a pair, keep the pair and reroll the kicker, hoping for three of a kind;</li>
 *   <li>otherwise keep one 4, one 2 and one 1 (the makings of a 421),
 *       or just the highest die, and reroll the rest.</li>
 * </ul>
 */
public final class AiPlayerController implements PlayerController {

    private static final Set<Integer> FOUR_TWO_ONE_VALUES = Set.of(4, 2, 1);

    @Override
    public TurnDecision decide(TurnState state) {
        if (state.rollsRemaining() == 0) {
            return new TurnDecision.Stand();
        }
        Hand hand = HandEvaluator.evaluate(state.dice());
        return switch (hand.category()) {
            case FOUR_TWO_ONE, TRIPLE_ACES, THREE_OF_A_KIND, STRAIGHT -> new TurnDecision.Stand();
            case ONE_PAIR -> rerollKicker(state.dice());
            case HIGH_DICE -> keepToward421(state.dice());
        };
    }

    private static TurnDecision rerollKicker(int[] dice) {
        int kickerPosition;
        if (dice[0] == dice[1]) {
            kickerPosition = 2;
        } else if (dice[1] == dice[2]) {
            kickerPosition = 0;
        } else {
            kickerPosition = 1;
        }
        return new TurnDecision.Reroll(Set.of(kickerPosition));
    }

    private static TurnDecision keepToward421(int[] dice) {
        Set<Integer> keep = new HashSet<>();
        Set<Integer> keptValues = new HashSet<>();
        for (int i = 0; i < dice.length; i++) {
            if (FOUR_TWO_ONE_VALUES.contains(dice[i]) && keptValues.add(dice[i])) {
                keep.add(i);
            }
        }
        if (keep.isEmpty()) {
            keep.add(indexOfHighest(dice));
        }
        Set<Integer> reroll = new HashSet<>();
        for (int i = 0; i < dice.length; i++) {
            if (!keep.contains(i)) {
                reroll.add(i);
            }
        }
        return reroll.isEmpty() ? new TurnDecision.Stand() : new TurnDecision.Reroll(reroll);
    }

    private static int indexOfHighest(int[] dice) {
        int highest = 0;
        for (int i = 1; i < dice.length; i++) {
            if (dice[i] > dice[highest]) {
                highest = i;
            }
        }
        return highest;
    }
}
