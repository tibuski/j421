package com.game421.player;

/**
 * A participant of the game, identified by its name and carrying its score
 * for the current match.
 */
public final class Player {

    private final String name;
    private int score;

    /**
     * Creates a player with the given name.
     *
     * @param name the player name; must not be blank
     * @throws IllegalArgumentException if the name is null or blank
     */
    public Player(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name must not be blank");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    /** Awards one point to this player. */
    public void addPoint() {
        score++;
    }

    @Override
    public String toString() {
        return name;
    }
}
