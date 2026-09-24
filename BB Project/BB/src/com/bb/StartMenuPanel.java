package com.bb;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class StartMenuPanel extends JPanel {

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final Frames frames;

    private final JButton loadButton;

    public StartMenuPanel(CardLayout cl, JPanel cards, Frames frames) {
        this.cardLayout = cl;
        this.cardPanel = cards;
        this.frames = frames;

        setLayout(new GridBagLayout());
        setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        // Title image
        JLabel titleImageSlot = new JLabel();
        ImageIcon titleIcon = loadIcon("/ui/title.png");
        if (titleIcon != null) {
            titleImageSlot.setIcon(titleIcon);
        } else {
            titleImageSlot.setText("BATTLESHIP");
            titleImageSlot.setForeground(Color.WHITE);
            titleImageSlot.setFont(new Font("SansSerif", Font.BOLD, 44));
            titleImageSlot.setPreferredSize(new Dimension(400, 100));
            titleImageSlot.setHorizontalAlignment(SwingConstants.CENTER);
        }
        gbc.gridy = 0;
        add(titleImageSlot, gbc);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        buttonPanel.setOpaque(false);

        JButton startButton = createImageButton("/ui/Start_BTN.png", "New Run");
        startButton.addActionListener(e -> {
            if (frames != null) {
                frames.startNewRun();
            } else {
                cardLayout.show(cardPanel, "Skills");
            }
        });

        loadButton = createImageButton(null, "Load Game");
        loadButton.addActionListener(e -> {
            if (frames != null) frames.promptLoad();
        });

        JButton exitButton = createImageButton("/ui/Exit_BTN.png", "Exit");
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(startButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(exitButton);

        gbc.gridy = 1;
        add(buttonPanel, gbc);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Only offer Load when there is actually something to load.
        if (frames != null) loadButton.setEnabled(frames.hasAnySave());
    }

    private JButton createImageButton(String path, String fallbackText) {
        JButton btn = new JButton();
        ImageIcon icon = path == null ? null : loadIcon(path);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
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
        return url != null ? new ImageIcon(url) : null;
    }
}
