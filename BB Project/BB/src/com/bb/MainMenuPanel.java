package com.bb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MainMenuPanel extends JPanel {

    private final CardLayout cl;
    private final JPanel cards;

    public MainMenuPanel(CardLayout cl, JPanel cards) {
        this.cl = cl;
        this.cards = cards;

        setLayout(new BorderLayout());
        add(new ResponsivePanel(), BorderLayout.CENTER);
    }

    class ResponsivePanel extends JPanel {

        private final Image bg       = loadImage("/background.png");
        private final Image logoBase = loadImage("/ui/title.png");

        private final JLabel title = new JLabel();
        private final StartButton btnStart = new StartButton();
        private final SettingsButton btnSettings = new SettingsButton();
        private final QuitButton btnQuit = new QuitButton();

        private static final int BASE_WIN_W = 900;
        private static final int BASE_WIN_H = 600;
        private static final int BASE_LOGO_W = 500;
        private static final int BASE_BTN_W  = 260;
        private static final int BASE_BTN_H  = 60;
        private static final int BASE_FONT   = 20;

        /** Last size we scaled for, so a resize does not rescale on every repaint. */
        private int lastScaledW = -1;
        private int lastScaledH = -1;

        private Image loadImage(String path) {
            java.net.URL url = Assets.getResource(path);
            if (url == null) {
                System.err.println("Image couldn't be found: " + path);
                return null;
            }
            return new ImageIcon(url).getImage();
        }

        ResponsivePanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnSettings.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnQuit.setAlignmentX(Component.CENTER_ALIGNMENT);

            add(Box.createVerticalGlue());
            add(title);
            add(Box.createVerticalStrut(10));
            add(btnStart);
            add(Box.createVerticalStrut(20));
            add(btnSettings);
            add(Box.createVerticalStrut(20));
            add(btnQuit);
            add(Box.createVerticalStrut(20));
            add(Box.createVerticalGlue());

            btnStart.addActionListener(e ->
                    MainMenuPanel.this.cl.show(MainMenuPanel.this.cards, "Skills"));
            btnSettings.addActionListener(e ->
                    MainMenuPanel.this.cl.show(MainMenuPanel.this.cards, "PAUSE_MENU"));
            btnQuit.addActionListener(e -> System.exit(0));

            // Rescaling belongs on resize, not on paint. Calling revalidate() from
            // paintComponent scheduled another layout pass, which repainted, which
            // rescaled again - an endless repaint loop that pegged a CPU core.
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateScaling(getWidth(), getHeight());
                }
            });

            updateScaling(BASE_WIN_W, BASE_WIN_H);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg != null) {
                g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        }

        private void updateScaling(int w, int h) {
            if (w <= 0 || h <= 0) return;
            if (w == lastScaledW && h == lastScaledH) return;
            lastScaledW = w;
            lastScaledH = h;

            double scale = Math.min(w / (double) BASE_WIN_W, h / (double) BASE_WIN_H);
            if (scale < 1.0) scale = 1.0;

            if (logoBase != null) {
                Image logoScaled = logoBase.getScaledInstance(
                        (int) (BASE_LOGO_W * scale), -1, Image.SCALE_SMOOTH);
                title.setIcon(new ImageIcon(logoScaled));
            }

            title.setBorder(BorderFactory.createEmptyBorder(
                    (int) (10 * scale), 0, (int) (30 * scale), 0));

            Dimension btnSize = new Dimension((int) (BASE_BTN_W * scale), (int) (BASE_BTN_H * scale));
            Font btnFont = new Font("Segoe UI", Font.BOLD, (int) (BASE_FONT * scale));

            for (JButton b : new JButton[]{btnStart, btnSettings, btnQuit}) {
                b.setFont(btnFont);
                b.setPreferredSize(btnSize);
                b.setMinimumSize(btnSize);
                b.setMaximumSize(btnSize);
            }

            revalidate();
        }
    }
}
