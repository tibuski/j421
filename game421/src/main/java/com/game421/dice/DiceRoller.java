package com.game421.dice;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Source of die rolls producing values between 1 and 6 inclusive.
 *
 * <p>Modelled as a functional interface so that game logic can be tested
 * with deterministic, scripted rollers.
 */
@FunctionalInterface
public interface DiceRoller {

    /** Number of sides on a standard die. */
    int SIDES = 6;

    /**
     * Rolls a single die.
     *
     * @return a value between 1 and {@value #SIDES} inclusive
     */
    int roll();

    /**
     * Returns a roller backed by a cryptographically strong random source.
     *
     * @return a secure random roller
     */
    static DiceRoller secure() {
        return of(new SecureRandom());
    }

    /**
     * Returns a roller backed by the given generator.
     *
     * @param generator the random source to use
     * @return a roller drawing values from {@code generator}
     */
    static DiceRoller of(RandomGenerator generator) {
        Objects.requireNonNull(generator, "generator must not be null");
        return () -> generator.nextInt(1, SIDES + 1);
    }
}
