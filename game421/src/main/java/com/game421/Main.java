package com.game421;

import com.game421.dice.DiceRoller;
import com.game421.game.Game;
import com.game421.game.GameConfig;
import com.game421.player.AiPlayerController;
import com.game421.player.Player;
import com.game421.player.PlayerController;
import com.game421.ui.ConsoleGameListener;
import com.game421.ui.ConsolePlayerController;
import com.game421.ui.SwingGame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * Entry point of the 421 dice game: parses the game setup from the console
 * and starts a match.
 */
public final class Main {

    private static final String RULES = """
            ========================================
               4 2 1  --  the classic dice game
            ========================================
            Each player rolls 3 dice. You have up to 3 rolls per
            turn: after each roll, set dice aside and reroll the
            rest, or keep your hand as it is.

            Hand rankings, from strongest to weakest:
              421
              1-1-1 (triple aces)
              Three of a kind (6-6-6 down to 2-2-2)
              Straight (6-5-4 down to 3-2-1)
              One pair (higher pair, then higher kicker)
              High dice

            The best hand wins the round and scores 1 point.
            Tied rounds are replayed.
            ========================================
            """;

    private Main() {
        // Entry point class: not instantiable.
    }

    public static void main(String[] args) {
        if (!java.util.Arrays.asList(args).contains("--console")) {
            SwingGame.launch();
            return;
        }
        PrintStream out = System.out;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        out.println(RULES);

        int mode = promptInt(in, out,
                "Choose a mode - 1: play against the computer, 2: two players [1]: ",
                1, value -> value == 1 || value == 2);
        int winningScore = promptInt(in, out,
                "Points needed to win the match [%d]: ".formatted(GameConfig.DEFAULT_WINNING_SCORE),
                GameConfig.DEFAULT_WINNING_SCORE, value -> value >= 1);

        Player first = new Player(promptText(in, out, "Name of player 1 [Player 1]: ", "Player 1"));
        Player second;

        Map<Player, PlayerController> controllers = new LinkedHashMap<>();
        ConsolePlayerController console = new ConsolePlayerController(in, out);
        controllers.put(first, console);
        if (mode == 1) {
            second = new Player("Computer");
            controllers.put(second, new AiPlayerController());
        } else {
            second = new Player(promptText(in, out, "Name of player 2 [Player 2]: ", "Player 2"));
            controllers.put(second, console);
        }

        Game game = new Game(List.of(first, second), controllers, DiceRoller.secure(),
                new GameConfig(winningScore), new ConsoleGameListener(out));
        game.play();
    }

    private static String promptText(BufferedReader in, PrintStream out, String message, String defaultValue) {
        out.print(message);
        out.flush();
        String line = readLine(in);
        return line == null || line.isBlank() ? defaultValue : line.trim();
    }

    private static int promptInt(BufferedReader in, PrintStream out, String message,
                                 int defaultValue, IntPredicate validator) {
        while (true) {
            out.print(message);
            out.flush();
            String line = readLine(in);
            if (line == null || line.isBlank()) {
                return defaultValue;
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (validator.test(value)) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Fall through to the error message below.
            }
            out.println("Invalid input, please try again.");
        }
    }

    private static String readLine(BufferedReader in) {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read input", e);
        }
    }
}
