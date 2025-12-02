package com.bb;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpponentPanel extends JPanel {

    private static final int SIZE = 8;
    private final JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2)) {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(2560, 1280);
        }
    };

    private boolean[][] firedOnOpponent = new boolean[SIZE][SIZE];
    private boolean[][] occupied       = new boolean[SIZE][SIZE];
    private boolean[][] firedOnPlayer  = new boolean[SIZE][SIZE];

    private GameLayout playerBoard;

    private final int maxShots = 3;
    private final List<Point> selectedShots = new ArrayList<>();
    private boolean confirmLocked = false;

    private final Color shotColorMiss = Color.RED;
    private final Color shotColorHit  = Color.YELLOW;

    public OpponentPanel() {
        setLayout(new BorderLayout());

        randomizeFleet();
        add(createBoardPanel(), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton confirmButton = new JButton("Confirm shots");
        confirmButton.addActionListener(e -> confirmShots());
        bottom.add(confirmButton);
        add(bottom, BorderLayout.SOUTH);

        setBorder(BorderFactory.createTitledBorder("Opponent Board"));
    }

    public void setPlayerBoard(GameLayout playerBoard) {
        this.playerBoard = playerBoard;
    }

    private JPanel createBoardPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        JPanel boardPanel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(1, SIZE + 1));
        top.add(new JLabel());
        for (int c = 0; c < SIZE; c++) {
            JLabel lbl = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            top.add(lbl);
        }
        outer.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        JPanel leftLabels = new JPanel(new GridLayout(SIZE, 1));
        for (int r = 1; r <= SIZE; r++) {
            JLabel lbl = new JLabel(String.valueOf(r), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            leftLabels.add(lbl);
        }
        center.add(leftLabels, BorderLayout.WEST);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));

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
    private void randomizeFleet() {
        int[] SHIP_SIZES = {5, 4, 3, 3, 2};
        java.util.Random rand = new java.util.Random();

        for (int size : SHIP_SIZES) {
            boolean placed = false;
            while (!placed) {
                boolean horizontal = rand.nextBoolean();
                int maxX = horizontal ? SIZE - size : SIZE - 1;
                int maxY = horizontal ? SIZE - 1 : SIZE - size;

                int x = rand.nextInt(maxX + 1);
                int y = rand.nextInt(maxY + 1);

                boolean ok = true;
                for (int i = 0; i < size; i++) {
                    int tx = horizontal ? x + i : x;
                    int ty = horizontal ? y : y + i;
                    if (occupied[ty][tx]) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) continue;

                for (int i = 0; i < size; i++) {
                    int tx = horizontal ? x + i : x;
                    int ty = horizontal ? y : y + i;
                    occupied[ty][tx] = true;
                }
                placed = true;
            }
        }
    }
    private void onCellClicked(int row, int col) {
        if (confirmLocked) return;
        if (firedOnOpponent[row][col]) return;
        if (selectedShots.size() >= maxShots) return;

        for (Point p : selectedShots) {
            if (p.x == col && p.y == row) return;
        }

        JButton cell = (JButton) grid.getComponent(row * SIZE + col);
        cell.setBackground(Color.LIGHT_GRAY);
        selectedShots.add(new Point(col, row));
    }
    private void confirmShots() {
        if (selectedShots.isEmpty()) return;

        confirmLocked = true;
        for (Point p : selectedShots) {
            int col = p.x;
            int row = p.y;

            firedOnOpponent[row][col] = true;

            JButton cell = (JButton) grid.getComponent(row * SIZE + col);
            boolean isHit = occupied[row][col];

            cell.setBackground(isHit ? shotColorHit : shotColorMiss);
        }
        if (playerBoard != null) {
            List<Point> shots = randomEnemyShots();
            playerBoard.applyShots(shots);
        }

        selectedShots.clear();
        confirmLocked = false;
    }

    private List<Point> randomEnemyShots() {
        List<Point> available = new ArrayList<>();

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!firedOnPlayer[r][c]) {
                    available.add(new Point(c, r));
                }
            }
        }
        Collections.shuffle(available);

        int n = Math.min(maxShots, available.size());
        List<Point> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Point p = available.get(i);
            firedOnPlayer[p.y][p.x] = true;
            result.add(p);
        }
        return result;
    }
}
