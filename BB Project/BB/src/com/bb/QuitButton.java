package com.bb;

import java.awt.*;
import javax.swing.*;

public class QuitButton extends GameButton {

    public QuitButton() {
        super("QUIT");
        addActionListener(e -> System.exit(0));
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
            top = new Color(150, 40, 30);
            bottom = new Color(110, 20, 10);
        } else if (hover) {

            top = new Color(200, 60, 50);
            bottom = new Color(170, 40, 30);
        } else {

            top = new Color(120, 50, 40);
            bottom = new Color(90, 30, 25);
        }

        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fillRoundRect(0, 0, w - 8, h - 8, arc, arc);

        g2.setColor(new Color(0, 0, 0, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(0, 0, w - 8, h - 8, arc, arc);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int tx = (w - 8 - textWidth) / 2;
        int ty = (h - 8 - fm.getHeight()) / 2 + textHeight;

        g2.setColor(Color.WHITE);
        g2.drawString(text, tx, ty);

        g2.dispose();
    }
}