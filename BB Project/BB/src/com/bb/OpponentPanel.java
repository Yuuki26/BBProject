

package com.bb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class OpponentPanel extends JPanel {
    private static final int SIZE = 8;

    public OpponentPanel() {
        this.setLayout(new BorderLayout());
        this.add(this.createBoard(), "Center");
        this.setBorder(BorderFactory.createTitledBorder("Opponent Board"));
    }

    private JComponent createBoard() {
        JPanel boardPanel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(1, 9));
        top.add(new JLabel());

        for(int c = 0; c < 8; ++c) {
            JLabel lbl = new JLabel(String.valueOf((char)(65 + c)), 0);
            lbl.setFont(lbl.getFont().deriveFont(1, 12.0F));
            top.add(lbl);
        }

        boardPanel.add(top, "North");
        JPanel center = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new GridLayout(8, 1));

        for(int r = 1; r <= 8; ++r) {
            JLabel lbl = new JLabel(String.valueOf(r), 0);
            lbl.setFont(lbl.getFont().deriveFont(1, 12.0F));
            left.add(lbl);
        }

        center.add(left, "West");
        JPanel grid = new JPanel(new GridLayout(8, 8, 2, 2)) {
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int side = Math.min(d.width, d.height);
                return new Dimension(side, side);
            }
        };

        for(int r = 0; r < 8; ++r) {
            for(int c = 0; c < 8; ++c) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
                String coord = "" + (char)(65 + c) + (r + 1);
                cell.setToolTipText(coord);
                grid.add(cell);
            }
        }

        center.add(grid, "Center");
        boardPanel.add(center, "Center");
        JPanel wrapper = new JPanel(new GridBagLayout());
        Dimension fixed = new Dimension(980, 980);
        boardPanel.setPreferredSize(fixed);
        boardPanel.setMinimumSize(fixed);
        boardPanel.setMaximumSize(fixed);
        wrapper.add(boardPanel);
        return wrapper;
    }
}
