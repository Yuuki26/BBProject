package com.bb;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class PauseMenuPanel extends JPanel {

    private JLabel pauseHeaderSlot;
    
    private JButton resumeButton;
    private JButton mainMenuButton;
    private JButton exitButton;

    private CardLayout cardLayout;
    private JPanel cardPanel;

    public PauseMenuPanel(CardLayout cl, JPanel cards) {
        this.cardLayout = cl;
        this.cardPanel = cards;
        setLayout(new GridBagLayout());
        setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Header Image
        pauseHeaderSlot = new JLabel();
        ImageIcon headerIcon = loadIcon("/images/pause_header.png");
        if (headerIcon != null) {
            pauseHeaderSlot.setIcon(headerIcon);
            pauseHeaderSlot.setHorizontalAlignment(SwingConstants.CENTER);
        } else {
            pauseHeaderSlot.setText("PAUSED");
            pauseHeaderSlot.setForeground(Color.WHITE);
            pauseHeaderSlot.setFont(new Font("SansSerif", Font.BOLD, 40));
            pauseHeaderSlot.setHorizontalAlignment(SwingConstants.CENTER);
        }
        gbc.gridy = 0;
        add(pauseHeaderSlot, gbc);

        // Button Container
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        buttonPanel.setOpaque(false);

        resumeButton = createImageButton("/ui/Ok_BTN.png", "Resume");
        resumeButton.addActionListener(e -> cardLayout.show(cardPanel, "PLAYER"));

        mainMenuButton = createImageButton("/ui/Menu_BTN.png", "Main Menu");
        mainMenuButton.addActionListener(e -> cardLayout.show(cardPanel, "START_MENU"));

        exitButton = createImageButton("/ui/Exit_BTN.png", "Exit Game");
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(resumeButton);
        buttonPanel.add(mainMenuButton);
        buttonPanel.add(exitButton);

        gbc.gridy = 1;
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
        } else {
            btn.setText(fallbackText);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(220, 45));
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

    @Override
    protected void paintComponent(Graphics g) {
        // Draw the semi-transparent background manually
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}