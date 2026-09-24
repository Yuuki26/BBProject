package com.bb;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class PauseMenuPanel extends JPanel {

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final Frames frames;

    private final JLabel runSummary = new JLabel("", SwingConstants.CENTER);

    public PauseMenuPanel(CardLayout cl, JPanel cards, Frames frames) {
        this.cardLayout = cl;
        this.cardPanel = cards;
        this.frames = frames;

        setLayout(new GridBagLayout());
        setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel pauseHeaderSlot = new JLabel();
        ImageIcon headerIcon = loadIcon("/images/pause_header.png");
        if (headerIcon != null) {
            pauseHeaderSlot.setIcon(headerIcon);
        } else {
            pauseHeaderSlot.setText("PAUSED");
            pauseHeaderSlot.setForeground(Color.WHITE);
            pauseHeaderSlot.setFont(new Font("SansSerif", Font.BOLD, 40));
        }
        pauseHeaderSlot.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        add(pauseHeaderSlot, gbc);

        runSummary.setForeground(new Color(255, 215, 120));
        runSummary.setFont(new Font("SansSerif", Font.PLAIN, 16));
        gbc.gridy = 1;
        add(runSummary, gbc);

        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        buttonPanel.setOpaque(false);

        JButton resumeButton = createImageButton("/ui/Ok_BTN.png", "Resume");
        resumeButton.addActionListener(e -> cardLayout.show(cardPanel, "PLAYER"));

        JButton saveButton = createImageButton(null, "Save Game");
        saveButton.addActionListener(e -> {
            if (frames != null) frames.promptSave();
        });

        JButton loadButton = createImageButton(null, "Load Game");
        loadButton.addActionListener(e -> {
            if (frames != null) frames.promptLoad();
        });

        JButton mainMenuButton = createImageButton("/ui/Menu_BTN.png", "Main Menu");
        mainMenuButton.addActionListener(e -> cardLayout.show(cardPanel, "START_MENU"));

        JButton exitButton = createImageButton("/ui/Exit_BTN.png", "Exit Game");
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(resumeButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(mainMenuButton);
        buttonPanel.add(exitButton);

        gbc.gridy = 2;
        add(buttonPanel, gbc);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        runSummary.setText(RunState.current().toString());
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
            btn.setFont(new Font("SansSerif", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(220, 45));
        }
        return btn;
    }

    private ImageIcon loadIcon(String path) {
        URL url = getClass().getResource(path);
        return url != null ? new ImageIcon(url) : null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
