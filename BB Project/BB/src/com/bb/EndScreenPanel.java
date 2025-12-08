package com.bb;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;

public class EndScreenPanel extends JPanel {
    private final CardLayout cl;
    private final JPanel cards;
    
    // Components
    private final JButton replayBtn;
    private final JButton closeBtn;
    private final JPanel contentPane;
    
    // State
    private boolean isVictory = false;
    
    // Assets
    private BufferedImage winWindow, winHeader, winStar1, winStar2, winStar3;
    private BufferedImage loseWindow, loseHeader;
    private ImageIcon winReplayIcon, winCloseIcon;
    private ImageIcon loseReplayIcon, loseCloseIcon;

    public EndScreenPanel(CardLayout cl, JPanel cards) {
        this.cl = cl;
        this.cards = cards;
        setLayout(new GridBagLayout());
        setBackground(new Color(0, 0, 0, 200)); // Dark overlay

        loadAssets();

        replayBtn = createButton(e -> cl.show(cards, "START_MENU"));
        closeBtn = createButton(e -> System.exit(0));

        // Custom painting panel for the window background
        contentPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Do not call super.paintComponent(g) so it's transparent
                drawWindow(g);
            }
        };
        contentPane.setOpaque(false);
        contentPane.setLayout(new GridBagLayout());
        
        add(contentPane);
    }

    private void loadAssets() {
        try {
            winWindow = loadImg("/You_Win/Window.png");
            winHeader = loadImg("/You_Win/Header.png");
            winStar1 = loadImg("/You_Win/Star_01.png");
            winStar2 = loadImg("/You_Win/Star_02.png");
            winStar3 = loadImg("/You_Win/Star_03.png");
            
            winReplayIcon = loadIcon("/You_Win/Replay_BTN.png");
            winCloseIcon = loadIcon("/You_Win/Close_BTN.png");

            loseWindow = loadImg("/You_Lose/Window.png");
            loseHeader = loadImg("/You_Lose/Header.png");
            
            loseReplayIcon = loadIcon("/You_Lose/Replay_BTN.png");
            loseCloseIcon = loadIcon("/You_Lose/Close_BTN.png");
        } catch (Exception e) {
            System.err.println("Failed to load assets: " + e.getMessage());
        }
    }

    private BufferedImage loadImg(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("Resource not found: " + path);
                return null;
            }
            return ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ImageIcon loadIcon(String path) {
        URL url = getClass().getResource(path);
        if (url == null) return null;
        return new ImageIcon(url);
    }

    private JButton createButton(java.awt.event.ActionListener l) {
        JButton b = new JButton();
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(l);
        return b;
    }

    public void setVictory(boolean victory) {
        this.isVictory = victory;
        updateUIState();
    }

    private void updateUIState() {
        // Set Icons
        if (isVictory) {
            if (winReplayIcon != null) replayBtn.setIcon(winReplayIcon);
            else replayBtn.setText("Replay");
            
            if (winCloseIcon != null) closeBtn.setIcon(winCloseIcon);
            else closeBtn.setText("Close");
        } else {
            if (loseReplayIcon != null) replayBtn.setIcon(loseReplayIcon);
            else replayBtn.setText("Retry");
            
            if (loseCloseIcon != null) closeBtn.setIcon(loseCloseIcon);
            else closeBtn.setText("Close");
        }

        // Calculate Scaled Dimensions
        BufferedImage bg = isVictory ? winWindow : loseWindow;
        BufferedImage header = isVictory ? winHeader : loseHeader;
        
        int w = 600; // Default fallback
        int h = 400;
        int headerOffset = 0;

        if (bg != null) {
            double maxW = 1000.0;
            double maxH = 650.0; // Leave room for taskbar/title bar/padding
            
            double imgW = bg.getWidth();
            double imgH = bg.getHeight();
            
            double scale = Math.min(1.0, Math.min(maxW / imgW, maxH / imgH));
            
            w = (int) (imgW * scale);
            h = (int) (imgH * scale);
            
            if (header != null) {
                // Scale header roughly to match window scale or keep it relative
                headerOffset = (int) ((header.getHeight() / 3.0) * scale); 
            }
        }

        contentPane.setPreferredSize(new Dimension(w, h + headerOffset));

        // Layout buttons
        contentPane.removeAll();
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Spacer to push buttons down
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0; 
        contentPane.add(Box.createGlue(), gbc);

        // Buttons row
        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        // Adjust insets scaled roughly
        int bottomPad = (int) (45 * (h / 1080.0)); // Scale padding relative to original height approx
        if (bottomPad < 10) bottomPad = 10;
        
        gbc.insets = new Insets(0, 15, bottomPad, 15); 
        gbc.anchor = GridBagConstraints.PAGE_END;

        contentPane.add(replayBtn, gbc);
        
        gbc.gridx = 1;
        contentPane.add(closeBtn, gbc);

        revalidate();
        repaint();
    }

    private void drawWindow(Graphics g) {
        int panelW = contentPane.getWidth();
        int panelH = contentPane.getHeight();
        
        BufferedImage bg = isVictory ? winWindow : loseWindow;
        BufferedImage header = isVictory ? winHeader : loseHeader;
        
        // Recalculate dimensions based on panel size (which respects preferred size)
        // We assume the panel size IS the target size essentially.
        
        int headerOffset = 0;
        double scale = 1.0;
        
        if (bg != null) {
             // Derive scale from current panel width vs original image width
             // width was set to (imgW * scale)
             scale = (double) panelW / bg.getWidth();
        }
        
        if (header != null) {
            headerOffset = (int) ((header.getHeight() / 3.0) * scale);
        }
        
        int bgY = headerOffset; 
        // Available height for background is total height - headerOffset
        int bgH = panelH - headerOffset;
        int bgW = panelW;

        if (bg != null) {
            g.drawImage(bg, 0, bgY, bgW, bgH, null);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRoundRect(0, bgY, bgW, bgH, 20, 20);
        }
        
        if (header != null) {
            // Draw header centered at top
            int hW = (int) (header.getWidth() * scale);
            int hH = (int) (header.getHeight() * scale);
            
            int hX = (panelW - hW) / 2;
            int hY = 0; // Top of panel
            
            g.drawImage(header, hX, hY, hW, hH, null);
            
            // Draw Stars if Victory
            if (isVictory && winStar1 != null) {
                int starW = (int) (winStar1.getWidth() * scale);
                int starH = (int) (winStar1.getHeight() * scale);
                int gap = (int) (5 * scale);
                int totalStarW = (starW * 3) + (gap * 2);
                int startX = (panelW - totalStarW) / 2;
                
                // Position stars relative to the header
                int starY = hY + hH - (int)(45 * scale); 
                
                g.drawImage(winStar1, startX, starY, starW, starH, null);
                g.drawImage(winStar2 != null ? winStar2 : winStar1, startX + starW + gap, starY - (int)(15 * scale), starW, starH, null);
                g.drawImage(winStar3 != null ? winStar3 : winStar1, startX + (starW + gap)*2, starY, starW, starH, null);
            }
        } else {
            // Text fallback
            g.setColor(isVictory ? Color.GREEN : Color.RED);
            g.setFont(new Font("Segoe UI", Font.BOLD, 40));
            String text = isVictory ? "VICTORY" : "DEFEAT";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (panelW - fm.stringWidth(text))/2, bgY + 50);
        }
    }
}