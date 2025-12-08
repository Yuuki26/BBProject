package com.bb;

import Ships.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpponentPanel extends JPanel {
    private static final int SIZE = 8;
    private final NormalEnenmy normale=new NormalEnenmy(SIZE);

    private final JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2)) {
        @Override public Dimension getPreferredSize() {
            return new Dimension(800, 800);
        }
    };

    private final boolean[][] firedOnOpponent = new boolean[SIZE][SIZE];
    private final boolean[][] occupied = new boolean[SIZE][SIZE];

    private GameLayout playerBoard;
    private Frames mainFrame;

    private final int maxShots = 3;
    private final List<Point> selectedShots = new ArrayList<>();
    private boolean confirmLocked = false;

    // selection / fire mode and penalty
    private boolean selectionEnabled = false;
    private int penaltyCount = 0;
    private final JLabel statusLabel = new JLabel("Ready");

    // colors
    private final Color shotColorMiss = Color.DARK_GRAY;
    private final Color shotColorHit  = Color.GREEN;
    private final Color shotColorPartial = Color.YELLOW;

    // Opponent fleet state
    private List<Ship_Placement> opponentPlacements = new ArrayList<>();
    private final Map<Ship_Placement, Integer> shipHP = new HashMap<>(); // remaining HP
    private final Map<Point, Ship_Placement> pointToShip = new HashMap<>(); // quick lookup

    public OpponentPanel() {
        setLayout(new BorderLayout());
        add(createBoardPanel(), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        JButton fireButton = new JButton("Fire");
        JButton confirmButton = new JButton("Confirm shots");

        fireButton.addActionListener(e -> toggleFireMode());
        confirmButton.addActionListener(e -> confirmShots());

        bottom.add(fireButton);
        bottom.add(confirmButton);
        bottom.add(statusLabel);

        add(bottom, BorderLayout.SOUTH);

        setBorder(BorderFactory.createTitledBorder("Opponent Board"));
    }

    public void setMainFrame(Frames frame) {
        this.mainFrame = frame;
    }

    /**
     * Must be called by the caller (Frames) after player board is created.
     * This wires the player's fleet into opponent generation.
     */
    public void setPlayerBoard(GameLayout playerBoard) {
        this.playerBoard = playerBoard;
        initOpponent(playerBoard.getFleet());
    }

    private JPanel createBoardPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        JPanel boardPanel = new JPanel(new BorderLayout());

        // Top labels
        JPanel top = new JPanel(new GridLayout(1, SIZE + 1));
        top.add(new JLabel());
        for (int c = 0; c < SIZE; c++) {
            JLabel lbl = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            top.add(lbl);
        }
        outer.add(top, BorderLayout.NORTH);

        // Left labels + grid
        JPanel center = new JPanel(new BorderLayout());
        JPanel leftLabels = new JPanel(new GridLayout(SIZE, 1));
        for (int r = 1; r <= SIZE; r++) {
            JLabel lbl = new JLabel(String.valueOf(r), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            leftLabels.add(lbl);
        }
        center.add(leftLabels, BorderLayout.WEST);

        // Build grid buttons
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
                String coord = "" + (char) ('A' + c) + (r + 1);
                cell.putClientProperty("coord", coord);

                final int row = r;
                final int col = c;
                cell.addActionListener(e -> onCellClicked(row, col));

                grid.add(cell);
            }
        }

        center.add(grid, BorderLayout.CENTER);
        boardPanel.add(center, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new GridBagLayout());
        Dimension fixed = new Dimension(800, 800);
        boardPanel.setPreferredSize(fixed);
        boardPanel.setMinimumSize(fixed);
        boardPanel.setMaximumSize(fixed);
        wrapper.add(boardPanel);

        outer.add(wrapper, BorderLayout.CENTER);
        return outer;
    }

    private void initOpponent(DefaultFleet playerFleet) {
        // clear previous state
        opponentPlacements.clear();
        shipHP.clear();
        pointToShip.clear();
        for (int r = 0; r < SIZE; r++) Arrays.fill(occupied[r], false);
        for (int r = 0; r < SIZE; r++) Arrays.fill(firedOnOpponent[r], false);

        penaltyCount = 0;
        statusLabel.setText("Ready");
        selectionEnabled = false;
        selectedShots.clear();

        // generate opponent placements based on player's fleet
        OpponentGenerator gen = new OpponentGenerator(SIZE);
        opponentPlacements = gen.buildOpponentFleet(playerFleet);

        // initialize ship HP and maps
        for (Ship_Placement sp : opponentPlacements) {
            Ships_Type s = sp.getShip();
            int hp = s.getHP();
            shipHP.put(sp, hp);
            for (Point p : sp.getOccupiedTiles()) {
                occupied[p.y][p.x] = true;
                // store a copy of the point as key to avoid mutation issues
                pointToShip.put(new Point(p.x, p.y), sp);
            }
        }

        // reset UI
        for (int i = 0; i < grid.getComponentCount(); i++) {
            JButton cell = (JButton) grid.getComponent(i);
            cell.setBackground(Color.WHITE);
            cell.setEnabled(true);
            cell.setText("");
        }
    }

    private void toggleFireMode() {
        if (selectionEnabled) {
            // cancel selection mode
            selectionEnabled = false;
            selectedShots.clear();
            clearSelectionUI();
            statusLabel.setText("Selection cancelled");
        } else {
            selectionEnabled = true;
            selectedShots.clear();
            clearSelectionUI();
            statusLabel.setText("Select up to " + maxShots + " targets then Confirm");
        }
    }

    private void clearSelectionUI() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!firedOnOpponent[r][c]) {
                    JButton cell = (JButton) grid.getComponent(r * SIZE + c);
                    cell.setBackground(Color.WHITE);
                }
            }
        }
    }

    private void onCellClicked(int row, int col) {
        if (!selectionEnabled) return;          // only allow selection when Fire enabled
        if (confirmLocked) return;

        if (firedOnOpponent[row][col]) {
            // penalty: trying to fire an already-fired cell
            penaltyCount++;
            statusLabel.setText("Already fired there. Penalty: " + penaltyCount);
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        Point p = new Point(col, row);
        // toggle selection (deselect if already selected)
        for (Point sp : new ArrayList<>(selectedShots)) {
            if (sp.equals(p)) {
                selectedShots.remove(sp);
                JButton cell = (JButton) grid.getComponent(row * SIZE + col);
                cell.setBackground(Color.WHITE);
                statusLabel.setText("Selected " + selectedShots.size() + "/" + maxShots);
                return;
            }
        }

        if (selectedShots.size() >= maxShots) {
            statusLabel.setText("Max targets selected (" + maxShots + ")");
            return;
        }

        selectedShots.add(p);
        JButton cell = (JButton) grid.getComponent(row * SIZE + col);
        cell.setBackground(Color.LIGHT_GRAY);
        statusLabel.setText("Selected " + selectedShots.size() + "/" + maxShots);
    }
    private void revealShipTiles(Ship_Placement ship, Color color, boolean markFired) {
        if (ship == null) return;
        for (Point tile : ship.getOccupiedTiles()) {
            int tx = tile.x;
            int ty = tile.y;
            if (tx < 0 || tx >= SIZE || ty < 0 || ty >= SIZE) continue;
            JButton cell = (JButton) grid.getComponent(ty * SIZE + tx);
            cell.setBackground(color);
            cell.setEnabled(!markFired ? true : false);
            if (markFired) {
                firedOnOpponent[ty][tx] = true;
            }
        }
    }
    private void confirmShots() {
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

        // compute damage hiddenly using player's fleet
        DefaultFleet playerFleet = playerBoard.getFleet();
        FleetCalculation calc = new FleetCalculation(playerFleet.getPlacements());
        float computedDamage = calc.DamageToShips(); // hidden from player

        confirmLocked = true;
        selectionEnabled = false; // disable further selection until next Fire
        statusLabel.setText("Applying shots...");

        for (Point p : new ArrayList<>(selectedShots)) {
            int col = p.x;
            int row = p.y;

            JButton cell = (JButton) grid.getComponent(row * SIZE + col);

            Ship_Placement hitShip = pointToShip.get(new Point(col, row));
            if (hitShip == null) {
                // miss -> permanently mark fired and disable
                firedOnOpponent[row][col] = true;
                cell.setBackground(shotColorMiss);
                cell.setEnabled(false);
            } else {
                // hit: reduce HP
                int remaining = shipHP.getOrDefault(hitShip, hitShip.getShip().getHP());
                int damage = Math.max(1, Math.round(computedDamage)); // at least 1 if >0
                remaining -= damage;
                shipHP.put(hitShip, remaining);

                int initialHP = hitShip.getShip().getHP();
                float pct = (float) remaining / (float) initialHP;

                if (remaining <= 0) {
                    // destroyed -> reveal all tiles red and mark them fired
                    revealShipTiles(hitShip, Color.RED, true);
                } else if (pct <= 0.5f) {
                    // dropped to 50% or below -> reveal all tiles yellow and mark them fired
                    revealShipTiles(hitShip, shotColorPartial, false);
                } else {
                    // still above 50% -> show green on this tile only, keep tile selectable for future shots
                    cell.setBackground(shotColorHit);
                    cell.setEnabled(true);
                    // do not mark firedOnOpponent for this tile so it can be targeted again
                }
            }
        }

        if (checkWinCondition()) {
            if (mainFrame != null) mainFrame.triggerGameOver(true); // Player Won!
            return; // Stop the code so AI doesn't fire back
        }

        // finalize
        selectedShots.clear();
        confirmLocked = false;
        statusLabel.setText("Wating for opponent");
        // short delay so player sees the result of their shots before AI fires
        // short delay so player sees the result of their shots before AI fires
        int delayMs = 800; // adjust as you like
        javax.swing.Timer t = new javax.swing.Timer(delayMs, evt -> {
            ((javax.swing.Timer) evt.getSource()).stop();

            List<Point> aiShots = normale.generateShots(maxShots);


            if (playerBoard != null) {

                playerBoard.applyShots(aiShots, opponentPlacements);
            }

            statusLabel.setText("Your turn");
        });
        t.setRepeats(false);
        t.start();
    }

    private boolean checkWinCondition() {
        // If map is empty, ships haven't generated yet, so not a win
        if (shipHP.isEmpty()) return false;

        for (int hp : shipHP.values()) {
            if (hp > 0) {
                return false; // Found a ship that is still alive
            }
        }
        return true; // All ships are <= 0 HP
    }
}
