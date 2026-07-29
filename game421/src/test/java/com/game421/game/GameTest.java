package com.game421.game;

import com.game421.dice.DiceRoller;
import com.game421.player.Player;
import com.game421.player.PlayerController;
import com.game421.turn.TurnDecision;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameTest {

    /** A controller that always keeps the initial roll. */
    private static final PlayerController ALWAYS_STAND = state -> new TurnDecision.Stand();

    /** Counts the events a game emits. */
    private static final class RecordingListener implements GameListener {
        private int roundsStarted;
        private int roundsTied;
        private int roundsWon;
        private int matchesWon;

        @Override
        public void roundStarted(int roundNumber) {
            roundsStarted++;
        }

        @Override
        public void roundTied(int roundNumber, Map<Player, com.game421.hand.Hand> hands) {
            roundsTied++;
        }

        @Override
        public void roundWon(int roundNumber, Player winner, Map<Player, com.game421.hand.Hand> hands) {
            roundsWon++;
        }

        @Override
        public void matchWon(Player champion) {
            matchesWon++;
        }
    }

    /** Returns a roller yielding the scripted values in order, round after round. */
    private static DiceRoller scriptedRoller(int... values) {
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

    private static Game game(Player first, Player second, DiceRoller roller,
                             int winningScore, GameListener listener) {
        return new Game(List.of(first, second),
                Map.of(first, ALWAYS_STAND, second, ALWAYS_STAND),
                roller, new GameConfig(winningScore), listener);
    }

    @Test
    void strongestHandWinsTheRoundAndTheMatch() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        RecordingListener listener = new RecordingListener();

        // Alice rolls three 6s, Bob rolls three 5s.
        Game game = game(alice, bob, scriptedRoller(6, 6, 6, 5, 5, 5), 1, listener);

        Player champion = game.play();

        assertSame(alice, champion);
        assertEquals(1, alice.getScore());
        assertEquals(0, bob.getScore());
        assertEquals(1, listener.roundsWon);
        assertEquals(1, listener.matchesWon);
    }

    @Test
    void tripleAcesBeatAnyOtherThreeOfAKind() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");

        // Alice rolls three 6s, Bob rolls three aces: Bob wins.
        Game game = game(alice, bob, scriptedRoller(6, 6, 6, 1, 1, 1), 1, new RecordingListener());

        assertSame(bob, game.play());
    }

    @Test
    void tiedRoundsAreReplayed() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        RecordingListener listener = new RecordingListener();

        // Round 1: both roll three 4s (tie). Replay: Alice three 2s, Bob three 3s.
        Game game = game(alice, bob,
                scriptedRoller(4, 4, 4, 4, 4, 4, 2, 2, 2, 3, 3, 3), 1, listener);

        Player champion = game.play();

        assertSame(bob, champion);
        assertEquals(1, listener.roundsTied);
        assertEquals(2, listener.roundsStarted);
        assertEquals(1, listener.roundsWon);
    }

    @Test
    void matchLastsUntilWinningScoreIsReached() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        RecordingListener listener = new RecordingListener();

        // Alice wins two rounds in a row.
        Game game = game(alice, bob,
                scriptedRoller(6, 6, 6, 5, 5, 5, 6, 6, 6, 5, 5, 5), 2, listener);

        Player champion = game.play();

        assertSame(alice, champion);
        assertEquals(2, alice.getScore());
        assertEquals(2, listener.roundsWon);
        assertEquals(1, listener.matchesWon);
    }

    @Test
    void controllerDecisionsAreApplied() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");

        // Alice rerolls everything once: her initial 5-5-5 becomes 4-2-1. Bob stands on three 6s.
        PlayerController rerollOnceThenStand = new PlayerController() {
            private boolean rerolled;

            @Override
            public TurnDecision decide(com.game421.turn.TurnState state) {
                if (!rerolled) {
                    rerolled = true;
                    return new TurnDecision.Reroll(java.util.Set.of(0, 1, 2));
                }
                return new TurnDecision.Stand();
            }
        };
        Game game = new Game(List.of(alice, bob),
                Map.of(alice, rerollOnceThenStand, bob, ALWAYS_STAND),
                scriptedRoller(5, 5, 5, 1, 2, 4, 6, 6, 6),
                new GameConfig(1), new RecordingListener());

        assertSame(alice, game.play());
    }

    @Test
    void rejectsFewerThanTwoPlayers() {
        Player solo = new Player("Solo");
        assertThrows(IllegalArgumentException.class, () -> new Game(
                List.of(solo), Map.of(solo, ALWAYS_STAND),
                scriptedRoller(1), new GameConfig(1), new RecordingListener()));
    }

    @Test
    void rejectsMissingController() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        assertThrows(IllegalArgumentException.class, () -> new Game(
                List.of(alice, bob), Map.of(alice, ALWAYS_STAND),
                scriptedRoller(1), new GameConfig(1), new RecordingListener()));
    }

    @Test
    void rejectsInvalidWinningScore() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(0));
    }
}
