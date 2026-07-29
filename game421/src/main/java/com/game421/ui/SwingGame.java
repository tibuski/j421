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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
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

    private static final Color BACKGROUND = new Color(29, 15, 9);
    private static final Color WOOD_DARK = new Color(51, 25, 13);
    private static final Color WOOD = new Color(94, 48, 24);
    private static final Color WOOD_LIGHT = new Color(132, 76, 38);
    private static final Color PARCHMENT = new Color(235, 213, 164);
    private static final Color PARCHMENT_SHADE = new Color(195, 162, 104);
    private static final Color INK = new Color(48, 29, 17);
    private static final Color TEXT = new Color(255, 239, 197);
    private static final Color MUTED = new Color(209, 178, 125);
    private static final Color ACCENT = new Color(218, 170, 67);
    private static final Color ACCENT_DARK = new Color(105, 40, 22);
    private static final Color GOLD = new Color(226, 183, 75);
    private static final Font DISPLAY_FONT = new Font(Font.SERIF, Font.BOLD, 18);
    private static final Font BODY_FONT = new Font(Font.SERIF, Font.PLAIN, 14);

    private final JFrame frame = new JFrame("421 · Dice Table");
    private final JLabel roundLabel = label("ROUND 1", 12, ACCENT);
    private final JLabel turnLabel = label("Preparing table...", 23, TEXT);
    private final JLabel hintLabel = label("", 13, MUTED);
    private final JLabel rollsLabel = label("", 18, ACCENT);
    private final JLabel handLabel = label("Roll the dice to begin", 18, TEXT);
    private final JLabel selectionLabel = label("KEEP: none", 13, ACCENT);
    private final JPanel roundDicePanel = new JPanel();
    private final JTextArea activity = new JTextArea();
    private final JPanel scorePanel = new JPanel(new GridLayout(1, 2, 12, 0));
    private final JButton[] diceButtons = new JButton[3];
    private final JButton rerollButton = button("REROLL OTHER DICE", ACCENT_DARK, TEXT);
    private final JButton standButton = button("STAND", GOLD, BACKGROUND);
    private final JButton nextRoundButton = button("NEXT ROUND", ACCENT, BACKGROUND);
    private final JButton playAgainButton = button("PLAY AGAIN", ACCENT, BACKGROUND);
    private final HumanController humanController = new HumanController();
    private final PlayerController secondController;
    private final Player human;
    private final Player second;
    private final int winningScore;
    private final Map<Player, int[]> lastDice = new LinkedHashMap<>();

    private boolean[] selected = new boolean[3];
    private boolean humanTurn;
    private int roundNumber = 1;
    private volatile CountDownLatch roundGate = new CountDownLatch(0);

    private SwingGame(String playerOne, String playerTwo, boolean computer, int winningScore) {
        this.human = new Player(playerOne);
        this.second = new Player(playerTwo);
        this.winningScore = winningScore;
        this.secondController = computer
                ? new LoggingController(second.getName(), new AiPlayerController())
                : humanController;
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

        JPanel root = new WoodPanel(new BorderLayout(24, 20), WOOD_DARK, BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        root.add(header(), BorderLayout.NORTH);
        root.add(table(), BorderLayout.CENTER);
        root.add(sidebar(), BorderLayout.EAST);
        frame.setContentPane(root);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                humanController.cancel();
                roundGate.countDown();
            }
        });
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = label("421", 46, TEXT);
        JLabel subtitle = label("THE GILDED DRAGON INN", 12, ACCENT);
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
        JPanel panel = new WoodPanel(new GridBagLayout(), WOOD, WOOD_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GOLD, 2),
                        BorderFactory.createLineBorder(WOOD_DARK, 4)),
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
            diceButtons[i] = new DieButton();
            diceButtons[i].setPreferredSize(new Dimension(125, 125));
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
        roundDicePanel.setOpaque(false);
        roundDicePanel.setLayout(new BoxLayout(roundDicePanel, BoxLayout.Y_AXIS));
        roundDicePanel.setVisible(false);
        panel.add(roundDicePanel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 8, 0);
        panel.add(selectionLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(rollsLabel, constraints);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        rerollButton.addActionListener(event -> submitReroll());
        standButton.addActionListener(event -> humanController.submit(new TurnDecision.Stand()));
        nextRoundButton.addActionListener(event -> continueToNextRound());
        playAgainButton.addActionListener(event -> playAgain());
        nextRoundButton.setVisible(false);
        playAgainButton.setVisible(false);
        actions.add(rerollButton);
        actions.add(standButton);
        actions.add(nextRoundButton);
        actions.add(playAgainButton);
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
        scoreBox.add(label("INN SCOREBOOK", 12, ACCENT), BorderLayout.NORTH);
        scoreBox.add(scorePanel, BorderLayout.CENTER);

        activity.setEditable(false);
        activity.setLineWrap(true);
        activity.setWrapStyleWord(true);
        activity.setFont(BODY_FONT.deriveFont(13f));
        activity.setForeground(INK);
        activity.setBackground(PARCHMENT);
        activity.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JScrollPane log = new JScrollPane(activity);
        log.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 2), BorderFactory.createLineBorder(WOOD_DARK, 3)));
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
        updateDieButton(position);
        rerollButton.setEnabled(hasDiceToReroll() && humanController.rollsRemaining() > 0);
        String state = selected[position] ? "held" : "released";
        append("Die " + (position + 1) + " (" + dieValue(position) + ") " + state + ". Keep: " + heldPositions());
        System.out.println("[421] Die " + (position + 1) + " " + state + "; keep positions " + heldPositions());
    }

    private void submitReroll() {
        if (!hasDiceToReroll()) {
            return;
        }
        Set<Integer> positions = new HashSet<>();
        for (int i = 0; i < selected.length; i++) {
            if (!selected[i]) {
                positions.add(i);
            }
        }
        append("Rerolling positions " + positions + "; keeping " + heldPositions() + ".");
        System.out.println("[421] Rerolling positions " + positions + "; keeping " + heldPositions());
        setControlsEnabled(false);
        humanController.submit(new TurnDecision.Reroll(positions));
    }

    private boolean hasDiceToReroll() {
        return !selected[0] || !selected[1] || !selected[2];
    }

    private void showTurn(Player player) {
        humanTurn = player == human;
        selected = new boolean[3];
        turnLabel.setText(player.getName() + " is rolling");
        hintLabel.setText(humanTurn ? "Click dice to keep, then reroll the others." : "The computer is thinking...");
        if (!humanTurn) {
            handLabel.setText("Computer's dice are hidden");
            selectionLabel.setText("KEEP: --");
            rollsLabel.setText("");
        }
        setControlsEnabled(humanTurn);
    }

    private void showDice(TurnState state) {
        // A new roll starts a fresh selection; the previous choices no longer apply.
        selected = new boolean[3];
        selectionLabel.setText("KEEP: none");
        roundDicePanel.setVisible(false);
        int[] dice = state.dice();
        for (int i = 0; i < diceButtons.length; i++) {
            ((DieButton) diceButtons[i]).setValue(dice[i]);
            updateDieButton(i);
        }
        rollsLabel.setText("ROLL " + state.rollsUsed() + " / 3  ·  " + state.rollsRemaining() + " REMAINING");
        if (humanTurn) {
            // Reroll submission temporarily disables the controls while the worker updates the turn.
            setControlsEnabled(true);
            rerollButton.setEnabled(state.rollsRemaining() > 0);
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

    private void updateDieButton(int position) {
        diceButtons[position].setBackground(selected[position] ? GOLD : PARCHMENT);
        diceButtons[position].setForeground(INK);
        selectionLabel.setText("KEEP: " + (heldPositions().isEmpty() ? "none" : heldPositions()));
    }

    private int dieValue(int position) {
        return ((DieButton) diceButtons[position]).value();
    }

    private Set<Integer> heldPositions() {
        Set<Integer> held = new HashSet<>();
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                held.add(i + 1);
            }
        }
        return held;
    }

    private void playAgain() {
        humanController.cancel();
        roundGate.countDown();
        frame.dispose();
        launch();
    }

    private void continueToNextRound() {
        nextRoundButton.setVisible(false);
        roundGate.countDown();
    }

    private void waitForNextRound() {
        try {
            roundGate.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
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
        card.setBackground(PARCHMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PARCHMENT_SHADE, 2), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel name = label(player.getName(), 12, INK);
        JLabel score = label(player.getScore() + " / " + winningScore, 23, player == human ? ACCENT_DARK : WOOD);
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
        label.setFont(DISPLAY_FONT.deriveFont((float) size));
        label.setForeground(color);
        return label;
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(DISPLAY_FONT.deriveFont(13f));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(48, 24, 12), 2),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        return button;
    }

    private static final class WoodPanel extends JPanel {
        private final Color top;
        private final Color bottom;

        private WoodPanel(java.awt.LayoutManager layout, Color top, Color bottom) {
            super(layout);
            this.top = top;
            this.bottom = bottom;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(255, 211, 139, 20));
            for (int y = 14; y < getHeight(); y += 24) {
                g.fillRect(0, y, getWidth(), 2);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class DieButton extends JButton {
        private int value;

        private DieButton() {
            setBackground(PARCHMENT);
            setForeground(INK);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
        }

        void setValue(int value) {
            this.value = value;
            repaint();
        }

        int value() {
            return value;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int inset = 4;
            int width = getWidth() - inset * 2;
            int height = getHeight() - inset * 2;
            g.setColor(getBackground());
            g.fillRoundRect(inset, inset, width, height, 18, 18);
            g.setColor(new Color(78, 42, 21));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(inset, inset, width, height, 18, 18);

            int pip = Math.max(5, Math.min(width, height) / 9);
            int left = inset + width / 4;
            int centerX = inset + width / 2;
            int right = inset + width * 3 / 4;
            int top = inset + height / 4;
            int centerY = inset + height / 2;
            int bottom = inset + height * 3 / 4;
            g.setColor(INK);
            switch (value) {
                case 1 -> pip(g, centerX, centerY, pip);
                case 2 -> { pip(g, left, top, pip); pip(g, right, bottom, pip); }
                case 3 -> { pip(g, left, top, pip); pip(g, centerX, centerY, pip); pip(g, right, bottom, pip); }
                case 4 -> { pip(g, left, top, pip); pip(g, right, top, pip); pip(g, left, bottom, pip); pip(g, right, bottom, pip); }
                case 5 -> { pip(g, left, top, pip); pip(g, right, top, pip); pip(g, centerX, centerY, pip); pip(g, left, bottom, pip); pip(g, right, bottom, pip); }
                case 6 -> { pip(g, left, top, pip); pip(g, right, top, pip); pip(g, left, centerY, pip); pip(g, right, centerY, pip); pip(g, left, bottom, pip); pip(g, right, bottom, pip); }
                default -> { }
            }
            g.dispose();
        }

        private void pip(Graphics2D g, int x, int y, int size) {
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
    }

    private final class Listener implements GameListener {
        @Override
        public void roundStarted(int number) {
            lastDice.clear();
            SwingUtilities.invokeLater(() -> {
                roundNumber = number;
                roundLabel.setText("ROUND " + number);
                append("Round " + number + " begins.");
            });
        }

        @Override
        public void turnStarted(Player player) {
            humanController.beginTurn();
            SwingUtilities.invokeLater(() -> {
                showTurn(player);
                append(player.getName() + "'s turn starts.");
            });
        }

        @Override
        public void diceRolled(Player player, TurnState state) {
            lastDice.put(player, state.dice());
            System.out.println("[421] " + player.getName() + " dice " + diceText(state.dice())
                    + "; roll " + state.rollsUsed() + "/3; remaining " + state.rollsRemaining());
            if (player != human) {
                SwingUtilities.invokeLater(() -> append("Computer rolled its dice."));
                return;
            }
            humanController.updateRolls(state.rollsRemaining());
            SwingUtilities.invokeLater(() -> {
                showDice(state);
                handLabel.setText(HandEvaluator.evaluate(state.dice()).describe());
            });
        }

        @Override
        public void turnEnded(Player player, Hand hand) {
            System.out.println("[421] " + player.getName() + " final dice " + diceText(lastDice.get(player))
                    + "; hand " + hand.describe());
            SwingUtilities.invokeLater(() -> append(player.getName() + "'s turn ends with " + hand.describe() + "."));
        }

        @Override
        public void roundTied(int number, Map<Player, Hand> hands) {
            SwingUtilities.invokeLater(() -> append("Tie in round " + number + ". Rolling again."));
        }

        @Override
        public void roundWon(int number, Player winner, Map<Player, Hand> hands) {
            roundGate = new CountDownLatch(1);
            System.out.println("[421] Point result: " + hands.entrySet().stream()
                    .map(entry -> entry.getKey().getName() + " " + diceText(lastDice.get(entry.getKey()))
                            + " = " + entry.getValue().describe())
                    .toList());
            System.out.println("[421] Point winner: " + winner.getName());
            SwingUtilities.invokeLater(() -> {
                humanTurn = false;
                setControlsEnabled(false);
                refreshScores();
                turnLabel.setText(winner.getName() + " wins the point!");
                hintLabel.setText("Final hands");
                handLabel.setText("Hands on the table");
                showRoundDice(hands, winner);
                rollsLabel.setText("POINT AWARDED");
                append(winner.getName() + " wins round " + number + " and scores a point.");
                nextRoundButton.setText(winner.getScore() >= winningScore ? "SHOW FINAL RESULT" : "NEXT ROUND");
                nextRoundButton.setVisible(true);
            });
            waitForNextRound();
        }

        private String roundHands(Map<Player, Hand> hands, Player winner) {
            StringBuilder result = new StringBuilder("<html>");
            for (Map.Entry<Player, Hand> entry : hands.entrySet()) {
                Player player = entry.getKey();
                String marker = player == winner ? "  * WINNER" : "";
                String color = player == winner ? "#3dd3b1" : "#eaf1f7";
                result.append("<font color='").append(color).append("'><b>")
                        .append(escapeHtml(player.getName())).append(marker)
                        .append("</b>  ")
                        .append(diceText(lastDice.get(player)))
                        .append("  -  ").append(escapeHtml(entry.getValue().describe()))
                        .append("</font><br>");
            }
            return result.append("</html>").toString();
        }

        private void showRoundDice(Map<Player, Hand> hands, Player winner) {
            roundDicePanel.removeAll();
            for (Map.Entry<Player, Hand> entry : hands.entrySet()) {
                Player player = entry.getKey();
                boolean won = player == winner;
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
                row.setOpaque(false);
                row.add(label(player.getName() + (won ? "  -  POINT WINNER" : ""), 14, won ? GOLD : TEXT));
                int[] dice = lastDice.get(player);
                if (dice != null) {
                    for (int value : dice) {
                        DieButton die = new DieButton();
                        die.setValue(value);
                        die.setEnabled(false);
                        die.setBackground(won ? GOLD : PARCHMENT);
                        die.setPreferredSize(new Dimension(48, 48));
                        row.add(die);
                    }
                }
                row.add(label(entry.getValue().describe(), 13, won ? GOLD : MUTED));
                roundDicePanel.add(row);
            }
            roundDicePanel.setVisible(true);
            roundDicePanel.revalidate();
            roundDicePanel.repaint();
        }

        private String diceText(int[] dice) {
            return dice == null ? "[? ? ?]" : "[" + dice[0] + "  " + dice[1] + "  " + dice[2] + "]";
        }

        private String escapeHtml(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        @Override
        public void matchWon(Player champion) {
            SwingUtilities.invokeLater(() -> {
                humanTurn = false;
                setControlsEnabled(false);
                playAgainButton.setVisible(true);
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
                CountDownLatch currentLatch = latch;
                currentLatch.await();
                TurnDecision result = decision == null ? new TurnDecision.Stand() : decision;
                decision = null;
                latch = new CountDownLatch(1);
                return result;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return new TurnDecision.Stand();
            }
        }
    }

    private static final class LoggingController implements PlayerController {
        private final String playerName;
        private final PlayerController delegate;

        private LoggingController(String playerName, PlayerController delegate) {
            this.playerName = playerName;
            this.delegate = delegate;
        }

        @Override
        public TurnDecision decide(TurnState state) {
            TurnDecision decision = delegate.decide(state);
            switch (decision) {
                case TurnDecision.Stand ignored -> System.out.println("[421] " + playerName + " keeps all dice and stands.");
                case TurnDecision.Reroll reroll -> {
                    Set<Integer> kept = new HashSet<>();
                    for (int i = 0; i < state.dice().length; i++) {
                        if (!reroll.positions().contains(i)) {
                            kept.add(i + 1);
                        }
                    }
                    System.out.println("[421] " + playerName + " keeps positions " + kept
                            + " and rerolls positions " + positionsForLog(reroll.positions()) + ".");
                }
            }
            return decision;
        }

        private Set<Integer> positionsForLog(Set<Integer> positions) {
            return positions.stream().map(position -> position + 1).collect(java.util.stream.Collectors.toSet());
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
