package com.bb;

import javax.swing.*;
import java.awt.*;

public class OpponentPanel extends JPanel {
    private static final int SIZE = 8;

    public OpponentPanel () {
        setLayout(new BorderLayout());
        add(createBoard(), BorderLayout.CENTER);
        setBorder(BorderFactory.createTitledBorder("Opponent Board"));
    }

    private JComponent createBoard() {
        JPanel boardPanel = new JPanel(new BorderLayout());
        // Top labels
        JPanel top = new JPanel(new GridLayout(1, SIZE + 1));
        top.add(new JLabel());
        for (int c = 0; c < SIZE; c++) {
            JLabel lbl = new JLabel(String.valueOf((char)('A' + c)), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
            top.add(lbl);
        }
        boardPanel.add(top, BorderLayout.NORTH);

        // Left labels + grid
        JPanel center = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new GridLayout(SIZE, 1));
        for (int r = 1; r <= SIZE; r++) {
            JLabel lbl = new JLabel(String.valueOf(r), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
            left.add(lbl);
        }
        center.add(left, BorderLayout.WEST);

        JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int side = Math.min(d.width, d.height);
                return new Dimension(side, side);
            }
        };
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
                String coord = "" + (char)('A' + c) + (r + 1);
                cell.setToolTipText(coord);
                // No action listener for opponent's board (empty)
                grid.add(cell);
            }
        }

        center.add(grid, BorderLayout.CENTER);
        boardPanel.add(center, BorderLayout.CENTER);
        JPanel wrapper = new JPanel(new GridBagLayout()); // centers its child
        Dimension fixed = new Dimension(980, 980);
        boardPanel.setPreferredSize(fixed);
        boardPanel.setMinimumSize(fixed);
        boardPanel.setMaximumSize(fixed);
        wrapper.add(boardPanel); // GridBagLayout centers by default

        return wrapper;
    }
    }

