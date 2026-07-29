package com.game421.game;

import com.game421.hand.Hand;
import com.game421.player.Player;
import com.game421.turn.TurnState;

import java.util.List;
import java.util.Map;

/**
 * Receives game events for presentation purposes. All methods are no-ops by
 * default so that listeners only implement the events they care about, and
 * tests can ignore presentation entirely.
 */
public interface GameListener {

    /** Called once when the match starts. */
    default void matchStarted(List<Player> players, int winningScore) {
    }

    /** Called when a round starts (also on replays after a tie). */
    default void roundStarted(int roundNumber) {
    }

    /** Called when a player starts their turn. */
    default void turnStarted(Player player) {
    }

    /** Called after each roll of a player's turn. */
    default void diceRolled(Player player, TurnState state) {
    }

    /** Called when a player finishes their turn with the given hand. */
    default void turnEnded(Player player, Hand hand) {
    }

    /** Called when a round ends in a tie and is about to be replayed. */
    default void roundTied(int roundNumber, Map<Player, Hand> hands) {
    }

    /** Called when a round is won; the winner has already been awarded a point. */
    default void roundWon(int roundNumber, Player winner, Map<Player, Hand> hands) {
    }

    /** Called once when the match ends. */
    default void matchWon(Player champion) {
    }
}
