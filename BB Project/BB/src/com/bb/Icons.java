package com.bb;

import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small image helpers shared by the shop, the starter picker, the reward screen and the board. */
final class Icons {

    /** The gold coin, drawn beside every price. */
    static final String COIN = "/Currency.png";

    private static final Map<String, ImageIcon> CACHE = new HashMap<>();

    /** Source art, loaded once per file. */
    private static final Map<String, Image> RAW = new HashMap<>();

    /**
     * Board art at the sizes it has been drawn at. Bounded, because resizing the window asks
     * for a new size every few pixels; the least recently drawn are dropped first.
     */
    private static final Map<String, BufferedImage> ART =
            new LinkedHashMap<String, BufferedImage>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
                    return size() > 48;
                }
            };

    private Icons() {}

    /** The coin at {@code size} pixels square, or null if the art is missing. */
    static ImageIcon coin(int size) {
        return fit(COIN, size, size);
    }

    /**
     * {@code path} scaled to fit inside {@code maxW} x {@code maxH} without stretching, or
     * null when the art is missing. Ship art is long and thin, so squashing it into a square
     * box would distort it.
     */
    static ImageIcon fit(String path, int maxW, int maxH) {
        if (path == null) return null;
        String key = path + "@" + maxW + "x" + maxH;
        if (CACHE.containsKey(key)) return CACHE.get(key);

        URL url = Assets.getResource(path);
        ImageIcon out = null;
        if (url != null) {
            ImageIcon raw = new ImageIcon(url);
            int w = raw.getIconWidth();
            int h = raw.getIconHeight();
            if (w > 0 && h > 0) {
                double scale = Math.min(maxW / (double) w, maxH / (double) h);
                int sw = Math.max(1, (int) Math.round(w * scale));
                int sh = Math.max(1, (int) Math.round(h * scale));
                out = new ImageIcon(raw.getImage().getScaledInstance(sw, sh, Image.SCALE_SMOOTH));
            }
        }
        CACHE.put(key, out);
        return out;
    }

    /**
     * Ship art for the board: {@code path} scaled to fit inside {@code maxW} x {@code maxH}
     * pixels without stretching, ready to be drawn turned or faded. {@code wreck} gives the
     * grey, darkened picture of a sunk ship. Null when the art is missing.
     *
     * <p>Sizes are rounded down to a multiple of 4, so a window being resized reuses a few
     * scalings instead of making a new one for every pixel.
     */
    static BufferedImage art(String path, int maxW, int maxH, boolean wreck) {
        if (path == null) return null;
        maxW -= maxW % 4;
        maxH -= maxH % 4;
        if (maxW <= 0 || maxH <= 0) return null;

        String key = path + "@" + maxW + "x" + maxH + (wreck ? "/wreck" : "");
        if (ART.containsKey(key)) return ART.get(key);

        Image raw = RAW.get(path);
        if (raw == null) {
            URL url = Assets.getResource(path);
            if (url == null) return null;
            raw = new ImageIcon(url).getImage();   // ImageIcon waits for it to load
            RAW.put(path, raw);
        }
        int w = raw.getWidth(null);
        int h = raw.getHeight(null);
        if (w <= 0 || h <= 0) return null;

        double scale = Math.min(maxW / (double) w, maxH / (double) h);
        int sw = Math.max(1, (int) Math.round(w * scale));
        int sh = Math.max(1, (int) Math.round(h * scale));
        Image scaled = new ImageIcon(raw.getScaledInstance(sw, sh, Image.SCALE_SMOOTH)).getImage();

        BufferedImage out = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();

        if (wreck) {
            // Grey, keeping the outline: what is left of it.
            for (int y = 0; y < sh; y++) {
                for (int x = 0; x < sw; x++) {
                    int argb = out.getRGB(x, y);
                    int r = (argb >> 16) & 0xff;
                    int gr = (argb >> 8) & 0xff;
                    int b = argb & 0xff;
                    int grey = (int) Math.round((0.30 * r + 0.59 * gr + 0.11 * b) * 0.8);
                    out.setRGB(x, y, (argb & 0xff000000) | (grey << 16) | (grey << 8) | grey);
                }
            }
        }
        ART.put(key, out);
        return out;
    }
}
