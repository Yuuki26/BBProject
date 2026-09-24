package com.bb;

import Ships.DefaultFleet;
import Ships.FleetCalculation;
import Ships.Ship_Placement;
import Ships.Ships_Type;
import skills.ModifiedStats;
import skills.SkillsRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The enemy board: where the player picks targets and fires.
 *
 * <p>One exchange is: press Fire to open selection, click up to
 * {@link #currentMaxShots()} tiles, then Confirm. The salvo resolves, and after a short
 * pause the enemy fires back at {@link GameLayout}.
 */
public class OpponentPanel extends JPanel {

    private static final int SIZE = 8;

    /** Stage from which the searching EliteEnemy replaces the random one. */
    private static final int ELITE_FROM_STAGE = 3;

    private final JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2)) {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(620, 620);
        }
    };

    private final JButton[][] cells = new JButton[SIZE][SIZE];
    private final boolean[][] firedOnOpponent = new boolean[SIZE][SIZE];

    private GameLayout playerBoard;
    private Frames mainFrame;

    /** Rebuilt each stage, so the enemy does not carry last stage's knowledge forward. */
    private EnemyAI enemyAI = new NormalEnenmy(SIZE);

    private final List<Point> selectedShots = new ArrayList<>();
    private boolean selectionEnabled = false;
    private boolean resolving = false;
    private int penaltyCount = 0;

    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel stageLabel = new JLabel("Stage 1");
    private final JButton fireButton = new JButton("Fire");
    private final JButton confirmButton = new JButton("Confirm shots");

    // colours
    private static final Color C_UNKNOWN = Color.WHITE;
    private static final Color C_SELECT  = Color.LIGHT_GRAY;
    private static final Color C_MISS    = Color.DARK_GRAY;
    private static final Color C_HIT     = new Color(60, 170, 70);
    private static final Color C_PARTIAL = new Color(225, 200, 70);
    private static final Color C_SUNK    = new Color(190, 55, 45);

    // Enemy fleet state
    private List<Ship_Placement> opponentPlacements = new ArrayList<>();
    private final Map<Ship_Placement, Integer> shipHP = new LinkedHashMap<>();
    private final Map<Point, Ship_Placement> pointToShip = new HashMap<>();

    public OpponentPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        add(createBoardPanel(), BorderLayout.CENTER);
        add(createControlBar(), BorderLayout.SOUTH);

        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "Opponent Board",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, null, Color.WHITE));
    }

    public void setMainFrame(Frames frame) {
        this.mainFrame = frame;
    }

    /** Wires in the player's board and generates the first stage's enemy fleet. */
    public void setPlayerBoard(GameLayout playerBoard) {
        this.playerBoard = playerBoard;
        startStage();
    }

    // =====================================================================================
    // UI
    // =====================================================================================

    private JPanel createControlBar() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bottom.setOpaque(false);

        fireButton.setFocusable(false);
        confirmButton.setFocusable(false);
        fireButton.addActionListener(e -> toggleFireMode());
        confirmButton.addActionListener(e -> confirmShots());

        stageLabel.setForeground(new Color(255, 215, 120));
        stageLabel.setFont(stageLabel.getFont().deriveFont(Font.BOLD, 14f));
        statusLabel.setForeground(Color.WHITE);

        bottom.add(stageLabel);
        bottom.add(fireButton);
        bottom.add(confirmButton);
        bottom.add(statusLabel);
        return bottom;
    }

    private JPanel createBoardPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel top = new JPanel(new GridLayout(1, SIZE));
        top.setOpaque(false);
        for (int c = 0; c < SIZE; c++) {
            top.add(axisLabel(String.valueOf((char) ('A' + c))));
        }
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(Box.createHorizontalStrut(24), BorderLayout.WEST);
        topRow.add(top, BorderLayout.CENTER);

        JPanel leftLabels = new JPanel(new GridLayout(SIZE, 1));
        leftLabels.setOpaque(false);
        for (int r = 1; r <= SIZE; r++) {
            leftLabels.add(axisLabel(String.valueOf(r)));
        }

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setOpaque(true);
                cell.setBackground(C_UNKNOWN);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
                cell.putClientProperty("coord", "" + (char) ('A' + c) + (r + 1));

                final int row = r;
                final int col = c;
                cell.addActionListener(e -> onCellClicked(row, col));

                cells[r][c] = cell;
                grid.add(cell);
            }
        }
        grid.setOpaque(false);

        JPanel boardPanel = new JPanel(new BorderLayout(4, 4));
        boardPanel.setOpaque(false);
        boardPanel.add(topRow, BorderLayout.NORTH);
        boardPanel.add(leftLabels, BorderLayout.WEST);
        boardPanel.add(grid, BorderLayout.CENTER);
        boardPanel.setPreferredSize(new Dimension(660, 660));

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(boardPanel);
        outer.add(wrapper, BorderLayout.CENTER);
        return outer;
    }

    private JLabel axisLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    // =====================================================================================
    // Stage lifecycle
    // =====================================================================================

    /** Generates a fresh enemy fleet for the stage the run is on and clears the board. */
    public void startStage() {
        opponentPlacements.clear();
        shipHP.clear();
        pointToShip.clear();
        selectedShots.clear();

        for (int r = 0; r < SIZE; r++) {
            Arrays.fill(firedOnOpponent[r], false);
        }

        penaltyCount = 0;
        selectionEnabled = false;
        resolving = false;

        int stage = RunState.current().getStage();
        enemyAI = createEnemyFor(stage);

        OpponentGenerator gen = new OpponentGenerator(SIZE);
        opponentPlacements = gen.buildOpponentFleet(
                playerBoard == null ? new DefaultFleet() : playerBoard.getFleet(), stage);

        for (Ship_Placement sp : opponentPlacements) {
            shipHP.put(sp, sp.getShip().getHP());
            for (Point p : sp.getOccupiedTiles()) {
                pointToShip.put(new Point(p.x, p.y), sp);
            }
        }

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c].setBackground(C_UNKNOWN);
                cells[r][c].setEnabled(true);
                cells[r][c].setText("");
            }
        }

        updateStageLabel();
        statusLabel.setText(opponentPlacements.size() + " enemy ships detected. Press Fire.");
    }

    /**
     * Picks the opponent for a stage.
     *
     * <p>Early stages face the random {@link NormalEnenmy}. From {@link #ELITE_FROM_STAGE}
     * the run switches to {@link EliteEnemy}, which searches the board properly and hunts
     * down a hit once it lands one - the point at which concealment starts mattering.
     */
    private EnemyAI createEnemyFor(int stage) {
        if (stage < ELITE_FROM_STAGE || playerBoard == null) {
            return new NormalEnenmy(SIZE);
        }
        return new EliteEnemy(SIZE);
    }

    private void updateStageLabel() {
        stageLabel.setText("Stage " + RunState.current().getStage()
                + "   Score " + RunState.current().getScore()
                + "   Enemy: " + enemyAI.getName());
    }

    /**
     * Shots available this salvo.
     *
     * <p>Delegates to the player's fleet rather than a constant, which is what makes the
     * count respond to upgrades, sunk ships and Rapid Fire.
     */
    private int currentMaxShots() {
        return playerBoard == null ? RunState.STARTING_SHOTS : playerBoard.getAvailableShots();
    }

    // =====================================================================================
    // Selection
    // =====================================================================================

    private void toggleFireMode() {
        if (resolving) return;

        if (playerBoard != null && !playerBoard.isReadyForBattle()) {
            statusLabel.setText("Deploy at least one ship first (press E for your board).");
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (selectionEnabled) {
            selectionEnabled = false;
            selectedShots.clear();
            clearSelectionUI();
            statusLabel.setText("Selection cancelled");
        } else {
            selectionEnabled = true;
            selectedShots.clear();
            clearSelectionUI();
            statusLabel.setText("Select up to " + currentMaxShots() + " targets, then Confirm");
        }
    }

    private void clearSelectionUI() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!firedOnOpponent[r][c] && cells[r][c].getBackground() == C_SELECT) {
                    cells[r][c].setBackground(C_UNKNOWN);
                }
            }
        }
    }

    private void onCellClicked(int row, int col) {
        if (!selectionEnabled || resolving) return;

        if (firedOnOpponent[row][col]) {
            penaltyCount++;
            statusLabel.setText("Already fired there. Wasted clicks: " + penaltyCount);
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        Point p = new Point(col, row);
        int max = currentMaxShots();

        if (selectedShots.remove(p)) {
            cells[row][col].setBackground(C_UNKNOWN);
            statusLabel.setText("Selected " + selectedShots.size() + "/" + max);
            return;
        }

        if (selectedShots.size() >= max) {
            statusLabel.setText("Max targets selected (" + max + ")");
            return;
        }

        selectedShots.add(p);
        cells[row][col].setBackground(C_SELECT);
        statusLabel.setText("Selected " + selectedShots.size() + "/" + max);
    }

    // =====================================================================================
    // Resolving a salvo
    // =====================================================================================

    private void confirmShots() {
        if (resolving) return;
        if (!selectionEnabled) {
            statusLabel.setText("Press Fire to enable selection");
            return;
        }
        if (selectedShots.isEmpty()) {
            statusLabel.setText("No targets selected");
            return;
        }
        if (playerBoard == null) {
            statusLabel.setText("Player board not set");
            return;
        }

        // Damage is computed from the player's fleet against the enemy's shields, with the
        // player's offensive skills folded in. It stays hidden from the player.
        float computedDamage = FleetCalculation.damageBetween(
                playerBoard.getAlivePlacements(),
                opponentPlacements,
                new ModifiedStats().dmgModifier(),
                1f);
        int perHit = Math.max(1, Math.round(computedDamage));

        resolving = true;
        selectionEnabled = false;
        statusLabel.setText("Applying shots...");

        for (Point p : new ArrayList<>(selectedShots)) {
            resolveShot(p.y, p.x, perHit);
        }

        selectedShots.clear();

        if (checkStageCleared()) {
            resolving = false;
            int hullLeft = remainingPlayerHull();
            statusLabel.setText("Enemy fleet destroyed!");
            if (mainFrame != null) mainFrame.triggerStageWon(hullLeft);
            return;
        }

        statusLabel.setText("Waiting for opponent...");

        // Brief pause so the player reads their own result before the reply lands.
        javax.swing.Timer t = new javax.swing.Timer(800, evt -> {
            ((javax.swing.Timer) evt.getSource()).stop();
            enemyTurn();
        });
        t.setRepeats(false);
        t.start();
    }

    private void resolveShot(int row, int col, int perHit) {
        JButton cell = cells[row][col];
        Ship_Placement hitShip = pointToShip.get(new Point(col, row));

        if (hitShip == null) {
            firedOnOpponent[row][col] = true;
            cell.setBackground(C_MISS);
            cell.setEnabled(false);
            return;
        }

        int remaining = shipHP.getOrDefault(hitShip, hitShip.getShip().getHP()) - perHit;
        shipHP.put(hitShip, Math.max(0, remaining));

        int initialHP = hitShip.getShip().getHP();
        float pct = (float) remaining / (float) initialHP;

        if (remaining <= 0) {
            revealShipTiles(hitShip, C_SUNK, true);
        } else if (pct <= 0.5f) {
            // Crippled: the whole hull shows, but it can still be shot at.
            revealShipTiles(hitShip, C_PARTIAL, false);
            firedOnOpponent[row][col] = true;
        } else {
            cell.setBackground(C_HIT);
            firedOnOpponent[row][col] = true;
        }
    }

    private void revealShipTiles(Ship_Placement ship, Color color, boolean markFired) {
        if (ship == null) return;
        for (Point tile : ship.getOccupiedTiles()) {
            if (tile.x < 0 || tile.x >= SIZE || tile.y < 0 || tile.y >= SIZE) continue;
            JButton cell = cells[tile.y][tile.x];
            cell.setBackground(color);
            if (markFired) {
                firedOnOpponent[tile.y][tile.x] = true;
                cell.setEnabled(false);
            }
        }
    }

    private void enemyTurn() {
        List<Point> aiShots = enemyAI.generateShots(enemyShotCount());
        if (playerBoard != null) {
            // Feeding the result back is what lets a searching opponent follow up on a hit.
            List<ShotOutcome> outcomes = playerBoard.applyShots(aiShots, opponentPlacements);
            enemyAI.reportResults(outcomes);
        }
        SkillsRegistry.tickTurnAll();

        // Submarines burn a point of oxygen per exchange, and surface when it runs out.
        if (playerBoard != null) {
            List<String> surfaced = playerBoard.tickSubmarines();
            if (!surfaced.isEmpty()) {
                statusLabel.setText(String.join("  ", surfaced));
            }
        }
        resolving = false;

        if (playerBoard != null && playerBoard.isDefeated()) {
            statusLabel.setText("Your fleet was destroyed.");
        } else {
            statusLabel.setText("Your turn. Press Fire.");
        }
    }

    /** The enemy fires more as the run goes on, but always fewer shots than a fresh player. */
    private int enemyShotCount() {
        int stage = RunState.current().getStage();
        return Math.min(6, 1 + stage / 2);
    }

    private boolean checkStageCleared() {
        if (shipHP.isEmpty()) return false;
        for (int hp : shipHP.values()) {
            if (hp > 0) return false;
        }
        return true;
    }

    private int remainingPlayerHull() {
        if (playerBoard == null) return 0;
        int sum = 0;
        for (Ship_Placement sp : playerBoard.getDeployedPlacements()) {
            sum += playerBoard.getShipHP(sp);
        }
        return sum;
    }

    // =====================================================================================
    // Save / load support
    // =====================================================================================

    public List<Ship_Placement> getOpponentPlacements() {
        return new ArrayList<>(opponentPlacements);
    }

    public int getOpponentShipHP(Ship_Placement sp) {
        return shipHP.getOrDefault(sp, 0);
    }

    public boolean[][] getFiredOnOpponent() {
        return firedOnOpponent;
    }

    /** Rebuilds the enemy board from a save instead of generating a new one. */
    public void restoreStage(List<Ship_Placement> placements,
                             List<Integer> hulls,
                             boolean[][] fired) {
        opponentPlacements = new ArrayList<>(placements);
        shipHP.clear();
        pointToShip.clear();
        selectedShots.clear();
        selectionEnabled = false;
        resolving = false;
        enemyAI = createEnemyFor(RunState.current().getStage());

        for (int i = 0; i < opponentPlacements.size(); i++) {
            Ship_Placement sp = opponentPlacements.get(i);
            Ships_Type type = sp.getShip();
            int hp = i < hulls.size() ? hulls.get(i) : type.getHP();
            shipHP.put(sp, hp);
            for (Point p : sp.getOccupiedTiles()) {
                pointToShip.put(new Point(p.x, p.y), sp);
            }
        }

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                boolean wasFired = fired != null && fired[r][c];
                firedOnOpponent[r][c] = wasFired;
                JButton cell = cells[r][c];
                cell.setText("");
                if (!wasFired) {
                    cell.setBackground(C_UNKNOWN);
                    cell.setEnabled(true);
                    continue;
                }
                // Repaint from what the save says is under that tile.
                Ship_Placement sp = pointToShip.get(new Point(c, r));
                if (sp == null) {
                    cell.setBackground(C_MISS);
                    cell.setEnabled(false);
                } else if (shipHP.getOrDefault(sp, 1) <= 0) {
                    cell.setBackground(C_SUNK);
                    cell.setEnabled(false);
                } else {
                    cell.setBackground(C_HIT);
                    cell.setEnabled(true);
                }
            }
        }

        updateStageLabel();
        statusLabel.setText("Game loaded. Press Fire.");
    }
}
