package com.bb;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class StartMenuPanel extends JPanel {

    private JLabel titleImageSlot;

    private JButton startButton;
    private JButton exitButton;

    private CardLayout cardLayout;
    private JPanel cardPanel;

    public StartMenuPanel(CardLayout cl, JPanel cards) {
        this.cardLayout = cl;
        this.cardPanel = cards;
        setLayout(new GridBagLayout());
        setBackground(Color.DARK_GRAY); 

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        //  Title Image
        titleImageSlot = new JLabel();
        ImageIcon titleIcon = loadIcon("/title.png");
        if (titleIcon != null) {
            titleImageSlot.setIcon(titleIcon);
        } else {
            titleImageSlot.setText("Missing: /images/title.png");
            titleImageSlot.setForeground(Color.WHITE);
            titleImageSlot.setFont(new Font("SansSerif", Font.BOLD, 24));
            titleImageSlot.setBorder(BorderFactory.createLineBorder(Color.WHITE));
            titleImageSlot.setPreferredSize(new Dimension(400, 100));
            titleImageSlot.setHorizontalAlignment(SwingConstants.CENTER);
        }
        gbc.gridy = 0;
        add(titleImageSlot, gbc);


        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        buttonPanel.setOpaque(false);

        startButton = createImageButton("/images/start_btn.png", "Start Game");
        startButton.addActionListener(e -> cardLayout.show(cardPanel, "PLAYER"));

        exitButton = createImageButton("/images/exit_btn.png", "Exit");
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(startButton);
        buttonPanel.add(exitButton);

        gbc.gridy = 2;
        add(buttonPanel, gbc);
    }

    private JButton createImageButton(String path, String fallbackText) {
        JButton btn = new JButton();
        ImageIcon icon = loadIcon(path);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            // Optionally set a rollover icon if you have one (e.g., path + "_hover.png")
        } else {
            btn.setText(fallbackText);
            btn.setFont(new Font("SansSerif", Font.BOLD, 18));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(200, 50));
        }
        return btn;
    }

    private ImageIcon loadIcon(String path) {
        URL url = getClass().getResource(path);
        if (url != null) {
            return new ImageIcon(url);
        }
        return null;
    }
}