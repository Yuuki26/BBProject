package com.bb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameButton extends JButton {

    protected boolean hover = false;
    protected boolean pressed = false;

    public GameButton(String text) {
        super(text);

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setHorizontalAlignment(CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 25;

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(4, 6, w - 8, h - 8, arc, arc);

        Color top, bottom;

        if (pressed) {

            top = new Color(30, 60, 100);
            bottom = new Color(15, 40, 70);
        } else if (hover) {

            top = new Color(60, 110, 180);
            bottom = new Color(40, 85, 150);
        } else {

            top = new Color(80, 80, 80);
            bottom = new Color(60, 60, 60);
        }

        GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, w - 8, h - 8, arc, arc);

        g2.setColor(new Color(0, 0, 0, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(0, 0, w - 8, h - 8, arc, arc);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();

        int tx = (w - 8 - textW) / 2;
        int ty = (h - 8 - fm.getHeight()) / 2 + textH;

        g2.setColor(getForeground());
        g2.drawString(text, tx, ty);

        g2.dispose();
    }
}