package com.game421.game;

import com.game421.dice.DiceRoller;
import com.game421.hand.Hand;
import com.game421.player.Player;
import com.game421.player.PlayerController;
import com.game421.turn.Turn;
import com.game421.turn.TurnDecision;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates a full match of 421: successive rounds in which every player
 * plays one turn. The strongest hand of a round scores one point; tied rounds
 * are replayed. The first player to reach the configured winning score wins
 * the match.
 */
public final class Game {

    private final List<Player> players;
    private final Map<Player, PlayerController> controllers;
    private final DiceRoller roller;
    private final GameConfig config;
    private final GameListener listener;

    /**
     * Creates a game.
     *
     * @param players     the participants, in playing order; at least two
     * @param controllers one controller per player
     * @param roller      the source of die values
     * @param config      the match settings
     * @param listener    receives game events for presentation
     * @throws IllegalArgumentException if there are fewer than two players or a
     *                                  player has no controller
     */
    public Game(List<Player> players, Map<Player, PlayerController> controllers, DiceRoller roller,
                GameConfig config, GameListener listener) {
        this.players = List.copyOf(Objects.requireNonNull(players, "players must not be null"));
        this.controllers = Map.copyOf(Objects.requireNonNull(controllers, "controllers must not be null"));
        this.roller = Objects.requireNonNull(roller, "roller must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
        if (this.players.size() < 2) {
            throw new IllegalArgumentException("A game requires at least 2 players");
        }
        for (Player player : this.players) {
            if (!this.controllers.containsKey(player)) {
                throw new IllegalArgumentException("Missing controller for player " + player.getName());
            }
        }
    }

    /**
     * Plays the match to completion.
     *
     * @return the champion
     */
    public Player play() {
        listener.matchStarted(players, config.winningScore());
        int roundNumber = 1;
        while (champion().isEmpty()) {
            playRound(roundNumber);
            roundNumber++;
        }
        Player champion = champion().orElseThrow();
        listener.matchWon(champion);
        return champion;
    }

    private void playRound(int roundNumber) {
        while (true) {
            listener.roundStarted(roundNumber);
            Map<Player, Hand> hands = new LinkedHashMap<>();
            for (Player player : players) {
                hands.put(player, playTurn(player));
            }
            Optional<Player> winner = uniqueHighestHand(hands);
            if (winner.isPresent()) {
                winner.get().addPoint();
                listener.roundWon(roundNumber, winner.get(), hands);
                return;
            }
            listener.roundTied(roundNumber, hands);
        }
    }

    private Hand playTurn(Player player) {
        PlayerController controller = controllers.get(player);
        Turn turn = new Turn(roller);

        listener.turnStarted(player);
        turn.start();
        listener.diceRolled(player, turn.snapshot());

        while (!turn.isFinished()) {
            TurnDecision decision = Objects.requireNonNull(
                    controller.decide(turn.snapshot()),
                    "Controller of player " + player.getName() + " returned a null decision");
            switch (decision) {
                case TurnDecision.Stand ignored -> turn.stand();
                case TurnDecision.Reroll reroll -> {
                    turn.reroll(reroll.positions());
                    listener.diceRolled(player, turn.snapshot());
                }
            }
        }

        Hand hand = turn.result();
        listener.turnEnded(player, hand);
        return hand;
    }

    private static Optional<Player> uniqueHighestHand(Map<Player, Hand> hands) {
        Hand best = hands.values().stream().max(Hand::compareTo).orElseThrow();
        List<Player> holders = hands.entrySet().stream()
                .filter(entry -> entry.getValue().equals(best))
                .map(Map.Entry::getKey)
                .toList();
        return holders.size() == 1 ? Optional.of(holders.getFirst()) : Optional.empty();
    }

    private Optional<Player> champion() {
        return players.stream()
                .filter(player -> player.getScore() >= config.winningScore())
                .findFirst();
    }
}
