package com.game421.ui;

import com.game421.player.PlayerController;
import com.game421.turn.Turn;
import com.game421.turn.TurnDecision;
import com.game421.turn.TurnState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * {@link PlayerController} asking a human player on the console which dice to
 * keep and which to reroll. Dice are referenced by their 1-based position.
 * An empty line (or end of input) keeps all dice.
 *
 * <p>The caller owns the reader: when several controllers or prompts read from
 * the same stream (e.g. {@code System.in}), they must share a single
 * {@link BufferedReader}, otherwise buffered input may be swallowed by one
 * reader and lost to the others.
 */
public final class ConsolePlayerController implements PlayerController {

    private final BufferedReader in;
    private final PrintStream out;

    public ConsolePlayerController(BufferedReader in, PrintStream out) {
        this.in = Objects.requireNonNull(in, "in must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    @Override
    public TurnDecision decide(TurnState state) {
        while (true) {
            out.printf("  Enter die positions to reroll (1-%d, space separated) or press Enter to keep all: ",
                    Turn.DICE_COUNT);
            out.flush();
            String line = readLine();
            if (line == null || line.isBlank()) {
                return new TurnDecision.Stand();
            }
            try {
                return new TurnDecision.Reroll(parsePositions(line));
            } catch (IllegalArgumentException e) {
                out.println("  Invalid input: " + e.getMessage());
            }
        }
    }

    private static Set<Integer> parsePositions(String line) {
        Set<Integer> positions = new LinkedHashSet<>();
        for (String token : line.trim().split("[\\s,]+")) {
            int position;
            try {
                position = Integer.parseInt(token);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + token + "' is not a die position");
            }
            if (position < 1 || position > Turn.DICE_COUNT) {
                throw new IllegalArgumentException("positions must be between 1 and " + Turn.DICE_COUNT);
            }
            positions.add(position - 1);
        }
        return positions;
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read player input", e);
        }
    }
}
