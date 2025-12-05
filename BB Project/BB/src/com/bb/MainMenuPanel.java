package com.bb;

import javax.swing.*;
import java.awt.*;

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

        private final Image bg       = loadImage("/background.jpg");
        private final Image logoBase = loadImage("/title.png");

        private Image loadImage(String path) {
            java.net.URL url = MainMenuPanel.class.getResource(path);
            if (url == null) {
                System.err.println("Image couldn't be found: " + path);
                return null;
            }
            return new ImageIcon(url).getImage();
        }


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

        public ResponsivePanel() {
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
                    MainMenuPanel.this.cl.show(MainMenuPanel.this.cards, "Skills")
            );

            btnSettings.addActionListener(e ->
                    MainMenuPanel.this.cl.show(MainMenuPanel.this.cards, "Skills")
            );

            btnQuit.addActionListener(e -> System.exit(0));


            updateScaling(BASE_WIN_W, BASE_WIN_H);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = getWidth();
            int h = getHeight();

            if (bg != null) {
                g.drawImage(bg, 0, 0, w, h, this);
            }

            updateScaling(w, h);
        }

        private void updateScaling(int w, int h) {
            if (w <= 0 || h <= 0) return;

            double scaleW = w / (double) BASE_WIN_W;
            double scaleH = h / (double) BASE_WIN_H;
            double scale  = Math.min(scaleW, scaleH);

            if (scale < 1.0) scale = 1.0;

            if (logoBase != null) {
                int logoW = (int) (BASE_LOGO_W * scale);
                Image logoScaled = logoBase.getScaledInstance(logoW, -1, Image.SCALE_SMOOTH);
                title.setIcon(new ImageIcon(logoScaled));
            }

            title.setBorder(BorderFactory.createEmptyBorder(
                    (int)(10 * scale), 0, (int)(30 * scale), 0
            ));

            int btnW = (int) (BASE_BTN_W * scale);
            int btnH = (int) (BASE_BTN_H * scale);
            int fontSize = (int) (BASE_FONT * scale);

            Dimension btnSize = new Dimension(btnW, btnH);
            Font btnFont = new Font("Segoe UI", Font.BOLD, fontSize);

            JButton[] buttons = { btnStart, btnSettings, btnQuit };

            for (JButton b : buttons) {
                b.setFont(btnFont);
                b.setPreferredSize(btnSize);
                b.setMinimumSize(btnSize);
                b.setMaximumSize(btnSize);
            }

            revalidate();
        }
    }
}
