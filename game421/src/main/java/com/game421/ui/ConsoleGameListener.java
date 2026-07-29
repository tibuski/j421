package com.game421.ui;

import com.game421.game.GameListener;
import com.game421.hand.Hand;
import com.game421.player.Player;
import com.game421.turn.TurnState;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link GameListener} printing match progress to a {@link PrintStream}.
 */
public final class ConsoleGameListener implements GameListener {

    private final PrintStream out;

    public ConsoleGameListener(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    @Override
    public void matchStarted(List<Player> players, int winningScore) {
        String duel = players.stream().map(Player::getName).collect(Collectors.joining(" vs "));
        out.printf("%n=== %s — first to %d point%s wins the match ===%n",
                duel, winningScore, winningScore > 1 ? "s" : "");
    }

    @Override
    public void roundStarted(int roundNumber) {
        out.printf("%n--- Round %d ---%n", roundNumber);
    }

    @Override
    public void turnStarted(Player player) {
        out.printf("%s's turn:%n", player.getName());
    }

    @Override
    public void diceRolled(Player player, TurnState state) {
        out.printf("  Roll %d: %s   (%d roll%s left)%n",
                state.rollsUsed(), formatDice(state.dice()),
                state.rollsRemaining(), state.rollsRemaining() == 1 ? "" : "s");
    }

    @Override
    public void turnEnded(Player player, Hand hand) {
        out.printf("  %s's hand: %s%n", player.getName(), hand.describe());
    }

    @Override
    public void roundTied(int roundNumber, Map<Player, Hand> hands) {
        out.printf("Round %d is tied (%s). Replaying the round.%n",
                roundNumber, hands.values().iterator().next().describe());
    }

    @Override
    public void roundWon(int roundNumber, Player winner, Map<Player, Hand> hands) {
        String score = hands.keySet().stream()
                .map(player -> player.getName() + " " + player.getScore())
                .collect(Collectors.joining(" - "));
        out.printf("Round %d goes to %s! Score: %s%n", roundNumber, winner.getName(), score);
    }

    @Override
    public void matchWon(Player champion) {
        out.printf("%n*** %s wins the match! ***%n", champion.getName());
    }

    private static String formatDice(int[] dice) {
        return Arrays.stream(dice)
                .mapToObj(value -> "[" + value + "]")
                .collect(Collectors.joining(" "));
    }
}
