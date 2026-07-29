package com.game421.ui;

import com.game421.dice.DiceRoller;
import com.game421.game.Game;
import com.game421.game.GameConfig;
import com.game421.game.GameListener;
import com.game421.hand.Hand;
import com.game421.hand.HandEvaluator;
import com.game421.player.AiPlayerController;
import com.game421.player.Player;
import com.game421.player.PlayerController;
import com.game421.turn.TurnDecision;
import com.game421.turn.TurnState;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/** Swing presentation for the game. Swing is part of the JDK and works on Windows and GNOME. */
public final class SwingGame {

    private static final Color BACKGROUND = new Color(13, 24, 38);
    private static final Color SURFACE = new Color(24, 39, 57);
    private static final Color SURFACE_LIGHT = new Color(35, 55, 77);
    private static final Color TEXT = new Color(234, 241, 247);
    private static final Color MUTED = new Color(154, 174, 191);
    private static final Color ACCENT = new Color(61, 211, 177);
    private static final Color ACCENT_DARK = new Color(27, 122, 108);
    private static final Color GOLD = new Color(245, 191, 66);

    private final JFrame frame = new JFrame("421 · Dice Table");
    private final JLabel roundLabel = label("ROUND 1", 12, ACCENT);
    private final JLabel turnLabel = label("Preparing table...", 23, TEXT);
    private final JLabel hintLabel = label("", 13, MUTED);
    private final JLabel rollsLabel = label("", 13, MUTED);
    private final JLabel handLabel = label("Roll the dice to begin", 18, TEXT);
    private final JTextArea activity = new JTextArea();
    private final JPanel scorePanel = new JPanel(new GridLayout(1, 2, 12, 0));
    private final JButton[] diceButtons = new JButton[3];
    private final JButton rerollButton = button("REROLL SELECTED", ACCENT_DARK, TEXT);
    private final JButton standButton = button("STAND", GOLD, BACKGROUND);
    private final HumanController humanController = new HumanController();
    private final PlayerController secondController;
    private final Player human;
    private final Player second;
    private final int winningScore;

    private boolean[] selected = new boolean[3];
    private boolean humanTurn;
    private int roundNumber = 1;

    private SwingGame(String playerOne, String playerTwo, boolean computer, int winningScore) {
        this.human = new Player(playerOne);
        this.second = new Player(playerTwo);
        this.winningScore = winningScore;
        this.secondController = computer ? new AiPlayerController() : humanController;
        buildWindow();
        refreshScores();
    }

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            Setup setup = Setup.show();
            if (setup == null) {
                return;
            }
            SwingGame game = new SwingGame(setup.playerOne(), setup.playerTwo(), setup.computer(), setup.winningScore());
            game.frame.setVisible(true);
            game.start();
        });
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setSize(1040, 700);
        frame.setLocationByPlatform(true);

        JPanel root = new JPanel(new BorderLayout(24, 20));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        root.add(header(), BorderLayout.NORTH);
        root.add(table(), BorderLayout.CENTER);
        root.add(sidebar(), BorderLayout.EAST);
        frame.setContentPane(root);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                humanController.cancel();
            }
        });
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = label("421", 42, TEXT);
        JLabel subtitle = label("THE CLASSIC DICE TABLE", 11, ACCENT);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);
        panel.add(titles, BorderLayout.WEST);
        panel.add(roundLabel, BorderLayout.EAST);
        return panel;
    }

    private JPanel table() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(49, 72, 94)),
                BorderFactory.createEmptyBorder(28, 28, 28, 28)));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(turnLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(7, 0, 0, 0);
        panel.add(hintLabel, constraints);

        JPanel dice = new JPanel(new GridLayout(1, 3, 14, 0));
        dice.setOpaque(false);
        for (int i = 0; i < diceButtons.length; i++) {
            int position = i;
            diceButtons[i] = button("-", SURFACE_LIGHT, TEXT);
            diceButtons[i].setPreferredSize(new Dimension(125, 125));
            diceButtons[i].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
            diceButtons[i].addActionListener(event -> toggleDie(position));
            dice.add(diceButtons[i]);
        }
        constraints.gridy++;
        constraints.insets = new Insets(34, 0, 18, 0);
        panel.add(dice, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 8, 0);
        panel.add(handLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(rollsLabel, constraints);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        rerollButton.addActionListener(event -> submitReroll());
        standButton.addActionListener(event -> humanController.submit(new TurnDecision.Stand()));
        actions.add(rerollButton);
        actions.add(standButton);
        constraints.gridy++;
        constraints.insets = new Insets(24, 0, 0, 0);
        panel.add(actions, constraints);
        return panel;
    }

    private JPanel sidebar() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(260, 0));

        JPanel scoreBox = new JPanel(new BorderLayout(0, 10));
        scoreBox.setOpaque(false);
        scoreBox.add(label("SCOREBOARD", 12, ACCENT), BorderLayout.NORTH);
        scoreBox.add(scorePanel, BorderLayout.CENTER);

        activity.setEditable(false);
        activity.setLineWrap(true);
        activity.setWrapStyleWord(true);
        activity.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        activity.setForeground(MUTED);
        activity.setBackground(SURFACE);
        activity.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JScrollPane log = new JScrollPane(activity);
        log.setBorder(BorderFactory.createLineBorder(new Color(49, 72, 94)));
        log.setPreferredSize(new Dimension(260, 260));

        panel.add(scoreBox, BorderLayout.NORTH);
        panel.add(log, BorderLayout.CENTER);
        return panel;
    }

    private void start() {
        append("A new match begins. First to " + winningScore + " points wins.");
        Thread gameThread = new Thread(() -> {
            Map<Player, PlayerController> controllers = new LinkedHashMap<>();
            controllers.put(human, humanController);
            controllers.put(second, secondController);
            Game game = new Game(List.of(human, second), controllers, DiceRoller.secure(),
                    new GameConfig(winningScore), new Listener());
            game.play();
        }, "421-game");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void toggleDie(int position) {
        if (!humanTurn) {
            return;
        }
        selected[position] = !selected[position];
        diceButtons[position].setBackground(selected[position] ? ACCENT : SURFACE_LIGHT);
        diceButtons[position].setForeground(selected[position] ? BACKGROUND : TEXT);
        rerollButton.setEnabled(hasSelection() && humanController.rollsRemaining() > 0);
    }

    private void submitReroll() {
        if (!hasSelection()) {
            return;
        }
        Set<Integer> positions = new HashSet<>();
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                positions.add(i);
            }
        }
        humanController.submit(new TurnDecision.Reroll(positions));
    }

    private boolean hasSelection() {
        return selected[0] || selected[1] || selected[2];
    }

    private void showTurn(Player player) {
        humanTurn = player == human;
        selected = new boolean[3];
        turnLabel.setText(player.getName() + " is rolling");
        hintLabel.setText(humanTurn ? "Select dice to reroll, or stand with this hand." : "The computer is thinking...");
        setControlsEnabled(humanTurn);
    }

    private void showDice(TurnState state) {
        int[] dice = state.dice();
        for (int i = 0; i < diceButtons.length; i++) {
            diceButtons[i].setText(Integer.toString(dice[i]));
            diceButtons[i].setBackground(SURFACE_LIGHT);
            diceButtons[i].setForeground(TEXT);
        }
        rollsLabel.setText("Roll " + state.rollsUsed() + " of 3  ·  " + state.rollsRemaining() + " remaining");
        if (humanTurn) {
            rerollButton.setEnabled(false);
            standButton.setEnabled(true);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        for (JButton die : diceButtons) {
            die.setEnabled(enabled);
        }
        rerollButton.setEnabled(false);
        standButton.setEnabled(enabled);
    }

    private void refreshScores() {
        scorePanel.removeAll();
        scorePanel.add(scoreCard(human));
        scorePanel.add(scoreCard(second));
        scorePanel.revalidate();
        scorePanel.repaint();
    }

    private JPanel scoreCard(Player player) {
        JPanel card = new JPanel(new BorderLayout(0, 3));
        card.setBackground(SURFACE_LIGHT);
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel name = label(player.getName(), 12, TEXT);
        JLabel score = label(player.getScore() + " / " + winningScore, 23, player == human ? ACCENT : GOLD);
        card.add(name, BorderLayout.NORTH);
        card.add(score, BorderLayout.CENTER);
        return card;
    }

    private void append(String message) {
        activity.append(message + "\n");
        activity.setCaretPosition(activity.getDocument().getLength());
    }

    private static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        label.setForeground(color);
        return label;
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
        return button;
    }

    private final class Listener implements GameListener {
        @Override
        public void roundStarted(int number) {
            SwingUtilities.invokeLater(() -> {
                roundNumber = number;
                roundLabel.setText("ROUND " + number);
                append("Round " + number + " begins.");
            });
        }

        @Override
        public void turnStarted(Player player) {
            humanController.beginTurn();
            SwingUtilities.invokeLater(() -> showTurn(player));
        }

        @Override
        public void diceRolled(Player player, TurnState state) {
            humanController.updateRolls(state.rollsRemaining());
            SwingUtilities.invokeLater(() -> {
                showDice(state);
                handLabel.setText(HandEvaluator.evaluate(state.dice()).describe());
            });
        }

        @Override
        public void turnEnded(Player player, Hand hand) {
            SwingUtilities.invokeLater(() -> append(player.getName() + " rolled " + hand.describe() + "."));
        }

        @Override
        public void roundTied(int number, Map<Player, Hand> hands) {
            SwingUtilities.invokeLater(() -> append("Tie in round " + number + ". Rolling again."));
        }

        @Override
        public void roundWon(int number, Player winner, Map<Player, Hand> hands) {
            SwingUtilities.invokeLater(() -> {
                refreshScores();
                append(winner.getName() + " wins round " + number + " and scores a point.");
            });
        }

        @Override
        public void matchWon(Player champion) {
            SwingUtilities.invokeLater(() -> {
                humanTurn = false;
                setControlsEnabled(false);
                turnLabel.setText(champion.getName() + " wins the match!");
                hintLabel.setText("Final score: " + human.getScore() + " - " + second.getScore());
                append("Match complete. Congratulations, " + champion.getName() + "!");
                JOptionPane.showMessageDialog(frame, champion.getName() + " wins the match!", "421 champion", JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    private static final class HumanController implements PlayerController {
        private volatile CountDownLatch latch = new CountDownLatch(0);
        private volatile TurnDecision decision;
        private volatile int rollsRemaining;

        void beginTurn() {
            decision = null;
            latch = new CountDownLatch(1);
        }

        void updateRolls(int remaining) {
            rollsRemaining = remaining;
        }

        int rollsRemaining() {
            return rollsRemaining;
        }

        void submit(TurnDecision next) {
            if (latch.getCount() == 0) {
                return;
            }
            decision = next;
            latch.countDown();
        }

        void cancel() {
            submit(new TurnDecision.Stand());
        }

        @Override
        public TurnDecision decide(TurnState state) {
            try {
                latch.await();
                return decision == null ? new TurnDecision.Stand() : decision;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return new TurnDecision.Stand();
            }
        }
    }

    private record Setup(String playerOne, String playerTwo, boolean computer, int winningScore) {
        static Setup show() {
            JTextField name = new JTextField("Player 1");
            JSpinner score = new JSpinner(new SpinnerNumberModel(GameConfig.DEFAULT_WINNING_SCORE, 1, 99, 1));
            Object[] fields = {"Your name:", name, "Points to win:", score};
            int result = JOptionPane.showConfirmDialog(null, fields, "Set up your match", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                return null;
            }
            String playerName = name.getText().isBlank() ? "Player 1" : name.getText().trim();
            return new Setup(playerName, "Computer", true, (Integer) score.getValue());
        }
    }
}
