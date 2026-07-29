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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Swing presentation for the game, dressed as a corner table of the Yawning
 * Portal inn in Waterdeep. Swing is part of the JDK and works on Windows and
 * GNOME.
 */
public final class SwingGame {

    // Candle-lit tavern palette.
    private static final Color BACKGROUND = new Color(24, 12, 7);
    private static final Color WOOD_DARK = new Color(44, 21, 11);
    private static final Color WOOD = new Color(86, 44, 22);
    private static final Color WOOD_LIGHT = new Color(128, 72, 36);
    private static final Color FELT = new Color(33, 66, 46);
    private static final Color FELT_DARK = new Color(20, 44, 30);
    private static final Color PARCHMENT = new Color(235, 213, 164);
    private static final Color PARCHMENT_DEEP = new Color(214, 185, 130);
    private static final Color PARCHMENT_SHADE = new Color(161, 124, 74);
    private static final Color INK = new Color(48, 29, 17);
    private static final Color TEXT = new Color(244, 226, 183);
    private static final Color MUTED = new Color(201, 168, 115);
    private static final Color ACCENT = new Color(218, 170, 67);
    private static final Color ACCENT_DARK = new Color(105, 40, 22);
    private static final Color GOLD = new Color(226, 183, 75);
    private static final Color GOLD_LIGHT = new Color(248, 222, 140);
    private static final Color GOLD_DEEP = new Color(160, 116, 40);
    private static final Color BONE = new Color(238, 227, 200);
    private static final Color BONE_SHADE = new Color(198, 180, 143);
    private static final Color WAX = new Color(148, 34, 28);

    private static final String DISPLAY_FAMILY = pickFantasyFamily();
    private static final Font DISPLAY_FONT = new Font(DISPLAY_FAMILY, Font.BOLD, 18);
    private static final Font BODY_FONT = new Font(DISPLAY_FAMILY, Font.PLAIN, 14);

    /** Well-known patrons of the Yawning Portal, ready for a game of dice. */
    private static final String[][] OPPONENTS = {
            {"Volo", "Volothamp \"Volo\" Geddarm, the infamous traveler"},
            {"Durnan", "Durnan Redblade, keeper of the Yawning Portal"},
            {"Mirt", "Mirt the Moneylender, old wolf of Waterdeep"},
            {"Laeral", "Laeral Silverhand, the Open Lord herself"},
    };

    /** Ambient tavern life, narrated by the bard between rounds. */
    private static final String[] AMBIENCE = {
            "A bard by the hearth strikes up \"The Dragon of the North\".",
            "The great well to Undermountain yawns in the middle of the taproom.",
            "Durnan polishes a tankard behind the bar, watching the dice.",
            "A merchant from Amn haggles over the price of saffron nearby.",
            "Rain patters against the leaded windows of the inn.",
            "A cloaked stranger in the corner pretends not to watch your table.",
    };

    private final JFrame frame = new JFrame("421 · The Yawning Portal — Waterdeep");
    private final GoldLabel roundLabel = GoldLabel.small("ROUND 1", 13, 0.25f);
    private final GoldLabel turnLabel = GoldLabel.heading("Lighting the candles...", 24);
    private final JLabel hintLabel = label("", 13, MUTED);
    private final GoldLabel rollsLabel = GoldLabel.small("", 14, 0.18f);
    private final JLabel handLabel = label("Cast the dice to begin", 19, TEXT);
    private final GoldLabel selectionLabel = GoldLabel.small("KEPT: none", 13, 0.18f);
    private JPanel tablePanel;
    private final JTextArea activity = new JTextArea();
    private final JPanel scorePanel = new JPanel(new GridLayout(1, 2, 12, 0));
    private final DieButton[] diceButtons = new DieButton[3];
    private final JButton rerollButton = TavernButton.wood("CAST THE REST AGAIN");
    private final JButton standButton = TavernButton.gold("LET THE HAND STAND");
    private final JButton nextRoundButton = TavernButton.gold("NEXT ROUND");
    private final JButton playAgainButton = TavernButton.gold("PLAY AGAIN");
    private final HumanController humanController = new HumanController();
    private final PlayerController secondController;
    private final Player human;
    private final Player second;
    private final String opponentTale;
    private final int winningScore;
    private final Map<Player, int[]> lastDice = new LinkedHashMap<>();
    private final Random ambience = new Random();
    private final Timer candleFlicker;

    private boolean[] selected = new boolean[3];
    private boolean humanTurn;
    private int roundNumber = 1;
    private volatile CountDownLatch roundGate = new CountDownLatch(0);

    private SwingGame(String playerOne, String[] opponent, int winningScore) {
        this.human = new Player(playerOne);
        this.second = new Player(opponent[0]);
        this.opponentTale = opponent[1];
        this.winningScore = winningScore;
        this.secondController = new LoggingController(second.getName(), new AiPlayerController());
        buildWindow();
        refreshScores();
        candleFlicker = new Timer(90, event -> frame.getContentPane().repaint());
        candleFlicker.start();
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
            SwingGame game = new SwingGame(setup.playerOne(), setup.opponent(), setup.winningScore());
            game.frame.setVisible(true);
            game.start();
        });
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980, 660));
        frame.setSize(1120, 760);
        frame.setLocationByPlatform(true);
        frame.setIconImage(frameIcon());

        JPanel root = new TavernPanel(new BorderLayout(26, 18));
        root.setBorder(BorderFactory.createEmptyBorder(22, 30, 26, 30));
        root.add(header(), BorderLayout.NORTH);
        root.add(table(), BorderLayout.CENTER);
        root.add(sidebar(), BorderLayout.EAST);
        frame.setContentPane(root);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                candleFlicker.stop();
                humanController.cancel();
                roundGate.countDown();
            }
        });
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        GoldLabel title = GoldLabel.heading("4 2 1", 46);
        title.setAlignmentX(0);
        GoldLabel subtitle = GoldLabel.small("THE YAWNING PORTAL · WATERDEEP", 13, 0.3f);
        subtitle.setAlignmentX(0);
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(subtitle);
        panel.add(titles, BorderLayout.WEST);

        JPanel roundBox = new JPanel(new BorderLayout());
        roundBox.setOpaque(false);
        roundBox.add(roundLabel, BorderLayout.NORTH);
        GoldLabel ledgerNote = GoldLabel.small("FIRST TO " + winningScore + " TAKES THE PURSE", 11, 0.2f);
        roundBox.add(ledgerNote, BorderLayout.SOUTH);
        panel.add(roundBox, BorderLayout.EAST);
        return panel;
    }

    private JPanel table() {
        JPanel panel = new FeltPanel(new GridBagLayout());
        tablePanel = panel;
        panel.setBorder(BorderFactory.createEmptyBorder(30, 34, 30, 34));

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

        JPanel dice = new JPanel(new GridLayout(1, 3, 6, 0));
        dice.setOpaque(false);
        for (int i = 0; i < diceButtons.length; i++) {
            int position = i;
            diceButtons[i] = new DieButton();
            diceButtons[i].setPreferredSize(new Dimension(124, 124));
            diceButtons[i].addActionListener(event -> toggleDie(position));
            // A padded, transparent cell gives the die room to tumble without clipping.
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(false);
            cell.setBorder(BorderFactory.createEmptyBorder(44, 0, 44, 0));
            cell.add(diceButtons[i]);
            dice.add(cell);
        }
        constraints.gridy++;
        constraints.insets = new Insets(36, 0, 20, 0);
        panel.add(dice, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 10, 0);
        handLabel.setFont(BODY_FONT.deriveFont(Font.ITALIC, 19f));
        panel.add(handLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 4, 0);
        panel.add(selectionLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(rollsLabel, constraints);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
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
        constraints.insets = new Insets(26, 0, 0, 0);
        panel.add(actions, constraints);
        return panel;
    }

    private JPanel sidebar() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(268, 0));

        JPanel scoreBox = new JPanel(new BorderLayout(0, 10));
        scoreBox.setOpaque(false);
        scoreBox.add(GoldLabel.small("THE LEDGER", 13, 0.3f), BorderLayout.NORTH);
        scorePanel.setOpaque(false);
        scoreBox.add(scorePanel, BorderLayout.CENTER);

        JPanel logBox = new JPanel(new BorderLayout(0, 10));
        logBox.setOpaque(false);
        logBox.add(GoldLabel.small("THE BARD'S TALE", 13, 0.3f), BorderLayout.NORTH);
        activity.setEditable(false);
        activity.setLineWrap(true);
        activity.setWrapStyleWord(true);
        activity.setFont(BODY_FONT.deriveFont(13.5f));
        activity.setForeground(INK);
        activity.setBackground(PARCHMENT);
        activity.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JScrollPane log = new JScrollPane(activity);
        log.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_DEEP, 2), BorderFactory.createLineBorder(WOOD_DARK, 3)));
        log.setPreferredSize(new Dimension(268, 300));
        logBox.add(log, BorderLayout.CENTER);

        panel.add(scoreBox, BorderLayout.NORTH);
        panel.add(logBox, BorderLayout.CENTER);
        panel.add(GoldLabel.small("MIND THE WELL TO UNDERMOUNTAIN", 10, 0.22f), BorderLayout.SOUTH);
        return panel;
    }

    private void start() {
        append("You take a seat at a scarred oak table in the Yawning Portal.");
        append("Across from you sits " + opponentTale + ". First to " + winningScore + " points claims the purse.");
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
        String state = selected[position] ? "held" : "freed";
        append((selected[position] ? "You hold die " : "You free die ") + (position + 1)
                + " (" + dieValue(position) + "). Kept: " + heldText());
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
        append("You cast the rest again — positions " + positions + "; keeping " + heldText() + ".");
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
        if (humanTurn) {
            turnLabel.setText(player.getName() + " takes the cup");
            hintLabel.setText("Tap a die to keep it, then cast the rest again.");
            for (DieButton die : diceButtons) {
                die.setRevealed(true);
                die.setKept(false);
            }
        } else {
            turnLabel.setText(player.getName() + " takes the cup");
            hintLabel.setText("The dice clatter behind a curled hand...");
            handLabel.setText(second.getName() + " keeps the dice hidden");
            selectionLabel.setText("KEPT: --");
            rollsLabel.setText("");
            for (DieButton die : diceButtons) {
                die.setRevealed(false);
            }
        }
        setControlsEnabled(humanTurn);
    }

    private void showDice(TurnState state) {
        // A new roll starts a fresh selection; dice kept from the last cast stay put.
        boolean[] wasKept = selected;
        selected = new boolean[3];
        selectionLabel.setText("KEPT: none");
        int[] dice = state.dice();
        for (int i = 0; i < diceButtons.length; i++) {
            diceButtons[i].setRevealed(true);
            diceButtons[i].setKept(false);
            if (wasKept[i]) {
                diceButtons[i].setValue(dice[i]);
            } else {
                diceButtons[i].animateTo(dice[i], i * 140);
            }
        }
        rollsLabel.setText("CAST " + state.rollsUsed() + " OF 3 · " + state.rollsRemaining() + " REMAIN");
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
        diceButtons[position].setKept(selected[position]);
        selectionLabel.setText("KEPT: " + heldText());
    }

    private int dieValue(int position) {
        return diceButtons[position].value();
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

    private String heldText() {
        Set<Integer> held = heldPositions();
        if (held.isEmpty()) {
            return "none";
        }
        return held.stream().sorted().map(String::valueOf).reduce((a, b) -> a + " & " + b).orElse("none");
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
        int lead = Math.max(human.getScore(), second.getScore());
        scorePanel.add(scoreCard(human, human.getScore() == lead && lead > 0));
        scorePanel.add(scoreCard(second, second.getScore() == lead && lead > 0));
        scorePanel.revalidate();
        scorePanel.repaint();
    }

    private JPanel scoreCard(Player player, boolean leading) {
        ParchmentPanel card = new ParchmentPanel(new BorderLayout(0, 2));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        if (leading) {
            card.setSeal(player.getName());
        }
        JLabel name = label(player.getName().toUpperCase(), 11, INK);
        card.add(name, BorderLayout.NORTH);
        JLabel score = label(player.getScore() + " / " + winningScore + " gp", 21,
                player == human ? ACCENT_DARK : WOOD);
        card.add(score, BorderLayout.CENTER);
        card.add(new CoinPurse(player.getScore(), winningScore), BorderLayout.SOUTH);
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

    private static String pickFantasyFamily() {
        String[] candidates = {"Palatino Linotype", "P052", "URW Palladio L", "Book Antiqua",
                "URW Bookman", "Georgia", "Times New Roman"};
        Set<String> available = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SERIF;
    }

    private static Image frameIcon() {
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(WOOD_DARK);
        g.fillRoundRect(0, 0, 64, 64, 14, 14);
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(3, 3, 58, 58, 12, 12);
        g.setColor(BONE);
        g.fillRoundRect(12, 12, 40, 40, 10, 10);
        g.setColor(INK);
        for (int[] pip : new int[][]{{22, 22}, {42, 22}, {32, 32}, {22, 42}, {42, 42}}) {
            g.fillOval(pip[0] - 4, pip[1] - 4, 8, 8);
        }
        g.dispose();
        return icon;
    }

    // ------------------------------------------------------------------
    // Painted components
    // ------------------------------------------------------------------

    /** The taproom: dark wooden planks, a vignette, and two flickering candle glows. */
    private static final class TavernPanel extends JPanel {
        private double phase;

        private TavernPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth();
            int h = getHeight();
            phase += 0.11;

            // Wooden planks, each board shaded a little differently.
            int plank = 46;
            for (int y = 0; y < h; y += plank) {
                Random grain = new Random(y / plank * 7919L);
                float tone = 0.85f + grain.nextFloat() * 0.3f;
                g.setColor(scale(WOOD_DARK, tone));
                g.fillRect(0, y, w, Math.min(plank, h - y));
                g.setColor(new Color(0, 0, 0, 90));
                g.fillRect(0, y, w, 2);
                g.setColor(new Color(255, 205, 140, 14));
                g.fillRect(0, y + 2, w, 1);
                g.setColor(new Color(0, 0, 0, 26));
                for (int line = 0; line < 4; line++) {
                    int gy = y + 6 + grain.nextInt(plank - 12);
                    g.fillRect(grain.nextInt(w / 2), gy, w / 3 + grain.nextInt(w / 2), 1);
                }
            }

            // Two warm candle glows falling from above.
            float flicker = (float) (0.86 + 0.10 * Math.sin(phase) + 0.04 * Math.sin(phase * 2.7 + 1.3));
            for (float cx : new float[]{w * 0.16f, w * 0.84f}) {
                float radius = h * 0.62f;
                int alpha = Math.round(46 * flicker);
                g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, -30), radius,
                        new float[]{0f, 1f},
                        new Color[]{new Color(255, 196, 96, alpha), new Color(255, 196, 96, 0)}));
                g.fillRect(0, 0, w, h);
            }

            // Vignette pressing the corners into shadow.
            float radius = (float) Math.hypot(w, h) * 0.62f;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.42f), radius,
                    new float[]{0.55f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 165)}));
            g.fillRect(0, 0, w, h);
            g.dispose();
            super.paintComponent(graphics);
        }

        private static Color scale(Color base, float factor) {
            return new Color(Math.min(255, Math.round(base.getRed() * factor)),
                    Math.min(255, Math.round(base.getGreen() * factor)),
                    Math.min(255, Math.round(base.getBlue() * factor)));
        }
    }

    /** The gaming table: a leather rim with gold tooling around candle-lit green felt. */
    private static final class FeltPanel extends JPanel {
        private FeltPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Leather rim.
            g.setPaint(new GradientPaint(0, 0, WOOD, 0, h, WOOD_DARK));
            g.fillRoundRect(0, 0, w, h, 26, 26);
            g.setColor(new Color(0, 0, 0, 120));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(1, 1, w - 3, h - 3, 24, 24);

            // Tooled gold line and stitching.
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(8, 8, w - 17, h - 17, 18, 18);
            g.setColor(new Color(226, 183, 75, 70));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{5, 6}, 0));
            g.drawRoundRect(13, 13, w - 27, h - 27, 14, 14);

            // The felt, lit by a lantern hanging above the table.
            g.setColor(FELT_DARK);
            g.fillRoundRect(17, 17, w - 34, h - 34, 10, 10);
            float radius = Math.max(w, h) * 0.72f;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.30f), radius,
                    new float[]{0f, 1f},
                    new Color[]{new Color(84, 128, 88, 235), FELT}));
            g.fillRoundRect(17, 17, w - 34, h - 34, 10, 10);

            // A faint inlaid diamond at the centre of the table.
            g.setColor(new Color(226, 183, 75, 20));
            g.setStroke(new BasicStroke(2));
            int cx = w / 2;
            int cy = h / 2;
            int d = Math.min(w, h) / 3;
            g.drawPolygon(new int[]{cx, cx + d, cx, cx - d}, new int[]{cy - d, cy, cy + d, cy}, 4);
            g.drawOval(cx - d / 2, cy - d / 2, d, d);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** A sheet of aged parchment, optionally stamped with a wax seal. */
    private static class ParchmentPanel extends JPanel {
        private String seal;

        ParchmentPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        void setSeal(String name) {
            seal = name;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setPaint(new GradientPaint(0, 0, PARCHMENT, 0, h, PARCHMENT_DEEP));
            g.fillRoundRect(0, 0, w, h, 8, 8);

            // Age spots.
            Random blots = new Random(421 + w * 31L + h);
            for (int i = 0; i < 7; i++) {
                g.setColor(new Color(120, 84, 40, 8 + blots.nextInt(12)));
                int bw = 14 + blots.nextInt(36);
                g.fillOval(blots.nextInt(Math.max(1, w - bw)), blots.nextInt(Math.max(1, h - 12)), bw, 10 + blots.nextInt(14));
            }

            // Darkened edges.
            g.setColor(new Color(120, 84, 40, 60));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(1, 1, w - 3, h - 3, 7, 7);

            if (seal != null) {
                paintWaxSeal(g, w - 34, 8, 26, Character.toUpperCase(seal.trim().charAt(0)));
            }
            g.dispose();
            super.paintComponent(graphics);
        }

        static void paintWaxSeal(Graphics2D g, int x, int y, int size, char letter) {
            Graphics2D sealGraphics = (Graphics2D) g.create();
            sealGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            sealGraphics.setColor(new Color(0, 0, 0, 70));
            sealGraphics.fillOval(x + 2, y + 3, size, size);
            sealGraphics.setPaint(new GradientPaint(x, y, new Color(178, 48, 38), x, y + size, WAX));
            sealGraphics.fillOval(x, y, size, size);
            sealGraphics.setColor(new Color(96, 18, 16));
            sealGraphics.setStroke(new BasicStroke(2));
            sealGraphics.drawOval(x + 2, y + 2, size - 5, size - 5);
            sealGraphics.setColor(new Color(245, 225, 170));
            sealGraphics.setFont(DISPLAY_FONT.deriveFont(Font.BOLD, size * 0.55f));
            var metrics = sealGraphics.getFontMetrics();
            String text = String.valueOf(letter);
            sealGraphics.drawString(text, x + (size - metrics.stringWidth(text)) / 2f,
                    y + (size + metrics.getAscent() - metrics.getDescent()) / 2f - 1);
            sealGraphics.dispose();
        }
    }

    /** A label painted in molten gold with letterspacing and an engraved shadow. */
    private static final class GoldLabel extends JComponent {
        private String text;
        private final float size;
        private final float tracking;
        private final Color top;
        private final Color bottom;

        private GoldLabel(String text, float size, float tracking, Color top, Color bottom) {
            this.text = text;
            this.size = size;
            this.tracking = tracking;
            this.top = top;
            this.bottom = bottom;
            setOpaque(false);
        }

        static GoldLabel heading(String text, float size) {
            return new GoldLabel(text, size, 0.06f, GOLD_LIGHT, GOLD_DEEP);
        }

        static GoldLabel small(String text, float size, float tracking) {
            return new GoldLabel(text, size, tracking, GOLD, new Color(150, 108, 38));
        }

        void setText(String text) {
            this.text = text;
            revalidate();
            repaint();
        }

        private Font font() {
            return DISPLAY_FONT.deriveFont(Font.BOLD, size);
        }

        private AttributedString attributed(Graphics2D g) {
            AttributedString attributed = new AttributedString(text, font().getAttributes());
            attributed.addAttribute(TextAttribute.TRACKING, tracking);
            return attributed;
        }

        @Override
        public Dimension getPreferredSize() {
            if (text == null || text.isEmpty()) {
                return new Dimension(10, Math.round(size));
            }
            Font font = font();
            var frc = new java.awt.font.FontRenderContext(null, true, true);
            TextLayout layout = new TextLayout(new AttributedString(text, font.getAttributes()).getIterator(), frc);
            var bounds = layout.getBounds();
            return new Dimension((int) Math.ceil(bounds.getWidth() + text.length() * size * tracking) + 8,
                    (int) Math.ceil(bounds.getHeight()) + 6);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (text == null || text.isEmpty()) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            TextLayout layout = new TextLayout(attributed(g).getIterator(), g.getFontRenderContext());
            var bounds = layout.getBounds();
            float y = (float) -bounds.getY() + 2;
            g.setColor(new Color(0, 0, 0, 150));
            layout.draw(g, 2, y + 2);
            g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            layout.draw(g, 0, y);
            g.dispose();
        }
    }

    /** A carved tavern button, in dark wood or in gold for the boldest moves. */
    private static final class TavernButton extends JButton {
        private final boolean golden;

        private TavernButton(String text, boolean golden) {
            super(text);
            this.golden = golden;
            setFont(DISPLAY_FONT.deriveFont(Font.BOLD, 13f));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setMargin(new Insets(10, 18, 10, 18));
        }

        static JButton wood(String text) {
            return new TavernButton(text, false);
        }

        static JButton gold(String text) {
            return new TavernButton(text, true);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension base = super.getPreferredSize();
            return new Dimension(base.width + 14, Math.max(base.height, 40));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            var model = getModel();
            boolean enabled = isEnabled();
            boolean pressed = model.isPressed();
            boolean hover = model.isRollover() && enabled;

            Color top;
            Color bottom;
            Color textColor;
            if (golden) {
                top = enabled ? (hover ? new Color(252, 228, 150) : GOLD_LIGHT) : new Color(140, 118, 76);
                bottom = enabled ? (pressed ? new Color(140, 100, 32) : GOLD_DEEP) : new Color(96, 82, 54);
                textColor = enabled ? new Color(56, 34, 12) : new Color(70, 62, 46);
            } else {
                top = enabled ? (hover ? new Color(122, 74, 40) : new Color(96, 56, 30)) : new Color(64, 44, 30);
                bottom = enabled ? (pressed ? new Color(44, 22, 12) : new Color(58, 30, 16)) : new Color(48, 34, 24);
                textColor = enabled ? TEXT : new Color(150, 132, 100);
            }

            g.setColor(new Color(0, 0, 0, pressed ? 40 : 110));
            g.fill(new RoundRectangle2D.Float(3, pressed ? 5 : 6, w - 6, h - 7, 12, 12));
            g.setPaint(new GradientPaint(0, 2, top, 0, h - 4, bottom));
            g.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 6, 12, 12));
            g.setColor(golden ? new Color(94, 62, 16) : GOLD_DEEP);
            g.setStroke(new BasicStroke(2));
            g.draw(new RoundRectangle2D.Float(2, 2, w - 5, h - 7, 12, 12));
            g.setColor(new Color(255, 240, 200, hover && enabled ? 70 : 34));
            g.setStroke(new BasicStroke(1));
            g.draw(new RoundRectangle2D.Float(4, 4, w - 9, h - 11, 10, 10));

            AttributedString attributed = new AttributedString(getText(), getFont().getAttributes());
            attributed.addAttribute(TextAttribute.TRACKING, 0.08f);
            TextLayout layout = new TextLayout(attributed.getIterator(), g.getFontRenderContext());
            var bounds = layout.getBounds();
            float x = (float) ((w - bounds.getWidth()) / 2 - bounds.getX());
            float y = (float) ((h - bounds.getHeight()) / 2 - bounds.getY()) + (pressed ? 1 : 0);
            g.setColor(new Color(0, 0, 0, golden ? 60 : 130));
            layout.draw(g, x + 1, y + 1);
            g.setColor(textColor);
            layout.draw(g, x, y);
            g.dispose();
        }
    }

    /**
     * A carved bone die. It tumbles for a heartbeat when cast, glows gold when
     * kept, and shows its dark leather back while the opponent hides it.
     */
    private static final class DieButton extends JButton {
        private int value;
        private int displayFace;
        private boolean kept;
        private boolean revealed = true;
        private boolean rolling;
        private double dx;
        private double dy;
        private double hop;
        private double angle;
        private double startDx;
        private double startDy;
        private double startSpin;
        private int bounceCount;
        private int rollTick;
        private Timer rollTimer;
        private final Random random = new Random();
        private static final int ROLL_TICKS = 28;

        private DieButton() {
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
        }

        void setValue(int value) {
            this.value = value;
            this.displayFace = value;
            repaint();
        }

        int value() {
            return value;
        }

        void setKept(boolean kept) {
            this.kept = kept;
            repaint();
        }

        void setRevealed(boolean revealed) {
            this.revealed = revealed;
            repaint();
        }

        /**
         * Casts the die across the felt: it slides in from the throw, bounces a
         * few times with a living shadow, spins down smoothly, flickers through
         * faces that slow as it settles, and lands on the true value.
         */
        void animateTo(int target, int delayMillis) {
            if (rollTimer != null && rollTimer.isRunning()) {
                rollTimer.stop();
            }
            value = target;
            displayFace = target;
            rolling = true;
            rollTick = 0;
            startDx = (random.nextDouble() - 0.5) * 48;
            startDy = 12 + random.nextDouble() * 12;
            startSpin = (random.nextDouble() - 0.5) * 2.6;
            bounceCount = 2 + random.nextInt(2);
            rollTimer = new Timer(33, null);
            rollTimer.setInitialDelay(delayMillis);
            rollTimer.addActionListener(event -> {
                rollTick++;
                double t = Math.min(1.0, rollTick / (double) ROLL_TICKS);
                double travel = 1.0 - easeOutCubic(t);
                dx = startDx * travel;
                dy = startDy * travel;
                angle = startSpin * travel;
                hop = Math.abs(Math.sin(t * Math.PI * bounceCount)) * 16.0 * (1.0 - t);
                // Faces flicker fast at first, then lock to the true face as the die lands.
                if (t < 0.68) {
                    int every = t < 0.34 ? 1 : 2;
                    if (rollTick % every == 0) {
                        int face;
                        do {
                            face = 1 + random.nextInt(6);
                        } while (face == displayFace);
                        displayFace = face;
                    }
                } else {
                    displayFace = value;
                }
                if (t >= 1.0) {
                    rollTimer.stop();
                    rolling = false;
                    dx = dy = hop = angle = 0;
                    displayFace = value;
                }
                repaint();
            });
            rollTimer.start();
        }

        private static double easeOutCubic(double t) {
            return 1.0 - Math.pow(1.0 - t, 3);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int inset = 8;
            int size = Math.min(w, h) - inset * 2;

            // The shadow follows the cast but shrinks and fades while the die is airborne.
            double shadowScale = 1.0 - Math.min(0.5, hop / 44.0);
            int shadowAlpha = (int) Math.round(95 * shadowScale);
            double shadowW = (w - inset * 4.0) * shadowScale;
            double shadowH = h * 0.30 * shadowScale;
            double shadowX = w / 2.0 + dx;
            double shadowY = h * 0.75 + dy;
            g.setPaint(new RadialGradientPaint(new Point2D.Float((float) shadowX, (float) shadowY),
                    (float) (shadowW / 2.0),
                    new float[]{0f, 1f},
                    new Color[]{new Color(0, 0, 0, shadowAlpha), new Color(0, 0, 0, 0)}));
            g.fill(new Ellipse2D.Double(shadowX - shadowW / 2, shadowY - shadowH / 2, shadowW, shadowH));

            Graphics2D die = (Graphics2D) g.create();
            die.translate(dx, dy - hop);
            if (rolling) {
                die.rotate(angle, w / 2.0, h / 2.0);
            }

            RoundRectangle2D body = new RoundRectangle2D.Float(inset, inset, size, size, 26, 26);
            if (revealed) {
                die.setPaint(new GradientPaint(inset, inset, new Color(246, 238, 214),
                        inset, inset + size, BONE_SHADE));
                die.fill(body);
                die.setColor(new Color(255, 252, 238, 160));
                die.setStroke(new BasicStroke(2));
                die.draw(new RoundRectangle2D.Float(inset + 2, inset + 2, size - 4, size * 0.45f, 22, 22));
                die.setColor(new Color(122, 96, 58));
                die.setStroke(new BasicStroke(2.5f));
                die.draw(body);
                int face = rolling ? displayFace : value;
                if (face >= 1 && face <= 6) {
                    paintPips(die, inset, size, face);
                }
            } else {
                // The dark leather back of the die while the opponent hides it.
                die.setPaint(new GradientPaint(inset, inset, new Color(70, 46, 30),
                        inset, inset + size, new Color(34, 20, 14)));
                die.fill(body);
                die.setColor(new Color(20, 12, 8));
                die.setStroke(new BasicStroke(2.5f));
                die.draw(body);
                die.setColor(new Color(226, 183, 75, 170));
                die.setStroke(new BasicStroke(2));
                int cx = inset + size / 2;
                int cy = inset + size / 2;
                int d = size / 6;
                die.drawPolygon(new int[]{cx, cx + d, cx, cx - d}, new int[]{cy - d, cy, cy + d, cy}, 4);
                die.drawOval(cx - d / 2, cy - d / 2, d, d);
            }

            if (kept && revealed) {
                die.setColor(new Color(226, 183, 75, 60));
                die.setStroke(new BasicStroke(9));
                die.draw(new RoundRectangle2D.Float(inset - 5, inset - 5, size + 10, size + 10, 30, 30));
                die.setColor(GOLD_LIGHT);
                die.setStroke(new BasicStroke(2.5f));
                die.draw(new RoundRectangle2D.Float(inset - 5, inset - 5, size + 10, size + 10, 30, 30));
            }
            die.dispose();
            g.dispose();
        }

        private void paintPips(Graphics2D g, int inset, int size, int face) {
            int pip = size / 8;
            int left = inset + size / 4;
            int centerX = inset + size / 2;
            int right = inset + size * 3 / 4;
            int top = inset + size / 4;
            int centerY = inset + size / 2;
            int bottom = inset + size * 3 / 4;
            int[][] spots = switch (face) {
                case 1 -> new int[][]{{centerX, centerY}};
                case 2 -> new int[][]{{left, top}, {right, bottom}};
                case 3 -> new int[][]{{left, top}, {centerX, centerY}, {right, bottom}};
                case 4 -> new int[][]{{left, top}, {right, top}, {left, bottom}, {right, bottom}};
                case 5 -> new int[][]{{left, top}, {right, top}, {centerX, centerY}, {left, bottom}, {right, bottom}};
                case 6 -> new int[][]{{left, top}, {right, top}, {left, centerY}, {right, centerY},
                        {left, bottom}, {right, bottom}};
                default -> new int[0][];
            };
            for (int[] spot : spots) {
                // Engraving: a pale crescent below the dark pip gives it depth.
                g.setColor(new Color(250, 244, 224, 130));
                g.fillOval(spot[0] - pip / 2, spot[1] - pip / 2 + 2, pip, pip);
                g.setColor(new Color(56, 36, 22));
                g.fillOval(spot[0] - pip / 2, spot[1] - pip / 2, pip, pip);
            }
        }
    }

    /** A row of gold coins showing the purse claimed so far. */
    private static final class CoinPurse extends JComponent {
        private final int earned;
        private final int goal;

        CoinPurse(int earned, int goal) {
            this.earned = earned;
            this.goal = goal;
            setOpaque(false);
            setPreferredSize(new Dimension(10, 16));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (goal > 12) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int d = 11;
            for (int i = 0; i < goal; i++) {
                int x = i * (d + 3);
                int y = 2;
                if (i < earned) {
                    g.setPaint(new GradientPaint(x, y, GOLD_LIGHT, x, y + d, GOLD_DEEP));
                    g.fillOval(x, y, d, d);
                    g.setColor(new Color(120, 84, 20));
                    g.drawOval(x, y, d, d);
                } else {
                    g.setColor(new Color(90, 62, 26, 110));
                    g.drawOval(x, y, d, d);
                }
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Round result and champion dialogs
    // ------------------------------------------------------------------

    private void showRoundResultDialog(Map<Player, Hand> hands, Player winner) {
        JDialog dialog = new JDialog(frame, "Round " + roundNumber + " — the tale is told", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                roundGate.countDown();
            }
        });

        JPanel root = new TavernPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_DEEP, 2), BorderFactory.createEmptyBorder(24, 30, 24, 30)));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        GoldLabel title = GoldLabel.heading(winner.getName() + " takes the purse!", 28);
        title.setAlignmentX(0);
        GoldLabel subtitle = GoldLabel.small("ROUND " + roundNumber + " · THE TABLE NODS IN AGREEMENT", 11, 0.28f);
        subtitle.setAlignmentX(0);
        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);
        root.add(heading, BorderLayout.NORTH);

        JPanel handsPanel = new JPanel();
        handsPanel.setOpaque(false);
        handsPanel.setLayout(new BoxLayout(handsPanel, BoxLayout.Y_AXIS));
        for (Map.Entry<Player, Hand> entry : hands.entrySet()) {
            handsPanel.add(resultCard(entry.getKey(), entry.getValue(), entry.getKey() == winner));
            handsPanel.add(Box.createVerticalStrut(14));
        }
        root.add(handsPanel, BorderLayout.CENTER);

        JButton continueButton = TavernButton.gold(
                winner.getScore() >= winningScore ? "SETTLE THE LEDGER" : "POUR THE NEXT ROUND");
        continueButton.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(continueButton);
        root.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(680, 480);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private JPanel resultCard(Player player, Hand hand, boolean winner) {
        ParchmentPanel card = new ParchmentPanel(new BorderLayout(14, 8));
        if (winner) {
            card.setSeal(player.getName());
        }
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(winner ? GOLD : PARCHMENT_SHADE, winner ? 3 : 2),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        card.setPreferredSize(new Dimension(600, 116));
        card.add(label(player.getName().toUpperCase() + (winner ? " · CLAIMS THE POINT" : ""), 14,
                winner ? ACCENT_DARK : INK), BorderLayout.NORTH);
        JPanel diceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        diceRow.setOpaque(false);
        int[] values = lastDice.get(player);
        if (values != null) {
            for (int value : values) {
                DieButton die = new DieButton();
                die.setValue(value);
                die.setEnabled(false);
                die.setPreferredSize(new Dimension(58, 58));
                diceRow.add(die);
            }
        }
        JLabel handName = label(hand.describe(), 15, INK);
        handName.setFont(BODY_FONT.deriveFont(Font.ITALIC, 15f));
        diceRow.add(handName);
        card.add(diceRow, BorderLayout.CENTER);
        return card;
    }

    private void showChampionDialog(Player champion) {
        JDialog dialog = new JDialog(frame, "A champion is hailed", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel root = new TavernPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_DEEP, 2), BorderFactory.createEmptyBorder(26, 32, 26, 32)));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        GoldLabel title = GoldLabel.heading("Hail " + champion.getName() + "!", 32);
        title.setAlignmentX(0);
        GoldLabel subtitle = GoldLabel.small("CHAMPION OF THE YAWNING PORTAL", 12, 0.3f);
        subtitle.setAlignmentX(0);
        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);
        root.add(heading, BorderLayout.NORTH);

        JLabel tale = label("<html>The inn raises a toast as the last purse slides across the oak.<br>"
                + "Final tally: " + human.getScore() + " – " + second.getScore()
                + ". The bard will sing of this night.</html>", 15, TEXT);
        root.add(tale, BorderLayout.CENTER);

        JButton close = TavernButton.gold("RAISE A TANKARD");
        close.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(520, 260);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Game event listener
    // ------------------------------------------------------------------

    private final class Listener implements GameListener {
        @Override
        public void roundStarted(int number) {
            lastDice.clear();
            SwingUtilities.invokeLater(() -> {
                roundNumber = number;
                roundLabel.setText("ROUND " + number);
                append("Round " + number + " — the table leans in.");
                if (number > 1 && ambience.nextInt(10) < 4) {
                    append(AMBIENCE[ambience.nextInt(AMBIENCE.length)]);
                }
            });
        }

        @Override
        public void turnStarted(Player player) {
            humanController.beginTurn();
            SwingUtilities.invokeLater(() -> {
                showTurn(player);
                append(player == human
                        ? "Your turn — the cup is yours."
                        : player.getName() + " rattles the cup, eyes narrowed.");
            });
        }

        @Override
        public void diceRolled(Player player, TurnState state) {
            lastDice.put(player, state.dice());
            System.out.println("[421] " + player.getName() + " dice " + diceText(state.dice())
                    + "; roll " + state.rollsUsed() + "/3; remaining " + state.rollsRemaining());
            if (player != human) {
                SwingUtilities.invokeLater(() -> append(player.getName() + "'s dice skitter from the cup."));
                return;
            }
            humanController.updateRolls(state.rollsRemaining());
            SwingUtilities.invokeLater(() -> {
                showDice(state);
                append("The dice clatter across the oak.");
                handLabel.setText(HandEvaluator.evaluate(state.dice()).describe());
            });
        }

        @Override
        public void turnEnded(Player player, Hand hand) {
            System.out.println("[421] " + player.getName() + " final dice " + diceText(lastDice.get(player))
                    + "; hand " + hand.describe());
            SwingUtilities.invokeLater(() -> append(player.getName() + " shows " + hand.describe() + "."));
        }

        @Override
        public void roundTied(int number, Map<Player, Hand> hands) {
            SwingUtilities.invokeLater(() -> append("A dead heat! Round " + number + " is cast again."));
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
                turnLabel.setText("The inn grows quiet...");
                hintLabel.setText("The hands are revealed upon the table.");
                handLabel.setText("Round " + number + " is done");
                rollsLabel.setText("");
                append(winner.getName() + " takes the purse of round " + number + ".");
                showRoundResultDialog(hands, winner);
            });
            waitForNextRound();
        }

        private String diceText(int[] dice) {
            return dice == null ? "[? ? ?]" : "[" + dice[0] + "  " + dice[1] + "  " + dice[2] + "]";
        }

        @Override
        public void matchWon(Player champion) {
            SwingUtilities.invokeLater(() -> {
                humanTurn = false;
                setControlsEnabled(false);
                playAgainButton.setVisible(true);
                turnLabel.setText(champion.getName() + " wins the match!");
                hintLabel.setText("Final tally: " + human.getScore() + " – " + second.getScore());
                append(champion.getName() + " is hailed champion of the Yawning Portal!");
                showChampionDialog(champion);
            });
        }
    }

    // ------------------------------------------------------------------
    // Controllers
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Setup dialog
    // ------------------------------------------------------------------

    private record Setup(String playerOne, String[] opponent, int winningScore) {
        static Setup show() {
            String[] opponent = OPPONENTS[new Random().nextInt(OPPONENTS.length)];

            JTextField name = new JTextField("Player 1");
            JSpinner score = new JSpinner(new SpinnerNumberModel(GameConfig.DEFAULT_WINNING_SCORE, 1, 99, 1));
            name.setFont(BODY_FONT.deriveFont(16f));
            name.setForeground(INK);
            name.setBackground(PARCHMENT);
            score.setFont(BODY_FONT.deriveFont(16f));
            ((JSpinner.DefaultEditor) score.getEditor()).getTextField().setForeground(INK);
            ((JSpinner.DefaultEditor) score.getEditor()).getTextField().setBackground(PARCHMENT);

            JDialog dialog = new JDialog((JFrame) null, "The Yawning Portal — Waterdeep", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            JPanel root = new TavernPanel(new BorderLayout(0, 18));
            root.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GOLD_DEEP, 2), BorderFactory.createEmptyBorder(24, 30, 24, 30)));
            JPanel heading = new JPanel();
            heading.setOpaque(false);
            heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
            GoldLabel title = GoldLabel.heading("The Yawning Portal", 30);
            title.setAlignmentX(0);
            GoldLabel subtitle = GoldLabel.small("WELL MET, TRAVELER · THE CITY OF SPLENDORS", 11, 0.28f);
            subtitle.setAlignmentX(0);
            heading.add(title);
            heading.add(Box.createVerticalStrut(6));
            heading.add(subtitle);
            heading.add(Box.createVerticalStrut(10));
            JLabel flavor = label("<html>Durnan nods toward a corner table, where " + opponent[1]
                    + " is already rattling the dice cup.</html>", 13, MUTED);
            flavor.setAlignmentX(0);
            heading.add(flavor);
            root.add(heading, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(PARCHMENT);
            form.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PARCHMENT_SHADE, 2), BorderFactory.createEmptyBorder(18, 20, 18, 20)));
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(0, 0, 8, 12);
            form.add(label("Adventurer name", 14, INK), constraints);
            constraints.gridx = 1;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            form.add(name, constraints);
            constraints.gridx = 0;
            constraints.gridy++;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(8, 0, 0, 12);
            form.add(label("Purse to claim (points)", 14, INK), constraints);
            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            form.add(score, constraints);
            root.add(form, BorderLayout.CENTER);

            final Setup[] selection = new Setup[1];
            JButton begin = TavernButton.gold("TAKE A SEAT");
            begin.addActionListener(event -> {
                String playerName = name.getText().isBlank() ? "Player 1" : name.getText().trim();
                selection[0] = new Setup(playerName, opponent, (Integer) score.getValue());
                dialog.dispose();
            });
            JButton leave = TavernButton.wood("WALK AWAY");
            leave.addActionListener(event -> dialog.dispose());
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            actions.setOpaque(false);
            actions.add(leave);
            actions.add(begin);
            root.add(actions, BorderLayout.SOUTH);

            dialog.setContentPane(root);
            dialog.setSize(560, 380);
            dialog.setResizable(false);
            dialog.setLocationByPlatform(true);
            dialog.setVisible(true);
            return selection[0];
        }
    }
}
