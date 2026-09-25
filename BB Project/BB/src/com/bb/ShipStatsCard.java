package com.bb;

import Ships.Ship_Placement;
import Ships.Ships_Type;
import Ships.Submarine;
import skills.ModifiedStats;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * The small stats board for one of the player's ships.
 *
 * <p>Opened by clicking a ship during a battle, or right-clicking one at any time, on the
 * board or in port. It shows the ship's picture, a hull bar, where it is and how badly it is
 * hit, and its numbers as they stand right now - shields and damage with skills applied, a
 * submarine's current detection and oxygen.
 *
 * <p>It floats over the window on the layered pane, so it never shifts the board; see
 * {@link GameLayout#showStats}. Clicking it closes it, as does <kbd>Esc</kbd>.
 */
public class ShipStatsCard extends JPanel {

    /** Hull bar colours: above half, at or below half, at or below a quarter, and sunk. */
    public static final Color HEALTHY = new Color(110, 205, 110);
    public static final Color CRIPPLED = new Color(225, 200, 70);
    public static final Color CRITICAL = new Color(225, 95, 70);
    public static final Color SUNK = new Color(125, 125, 135);

    private static final Color BG = new Color(22, 30, 44, 242);
    private static final Color GOLD_TEXT = new Color(255, 215, 120);
    private static final Color SOFT_TEXT = new Color(190, 205, 225);
    private static final Color BOOST = new Color(130, 220, 140);
    private static final int CARD_WIDTH = 290;

    private final JLabel name = new JLabel();
    private final JLabel hull = new JLabel();
    private final JLabel art = new JLabel("", SwingConstants.CENTER);
    private final HullBar bar = new HullBar();
    private final JLabel where = new JLabel();
    private final JLabel condition = new JLabel();
    private final JPanel grid = new JPanel(new GridLayout(0, 2, 14, 3));

    private Color accent = HEALTHY;
    private String conditionText = "";
    private String whereText = "";

    /** @param onClose run when the card is clicked */
    public ShipStatsCard(Runnable onClose) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        setVisible(false);

        name.setFont(name.getFont().deriveFont(Font.BOLD, 17f));
        name.setForeground(GOLD_TEXT);
        JLabel close = new JLabel("\u00d7");
        close.setFont(close.getFont().deriveFont(Font.BOLD, 18f));
        close.setForeground(SOFT_TEXT);
        close.setToolTipText("Close (Esc)");

        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(name, BorderLayout.CENTER);
        title.add(close, BorderLayout.EAST);

        hull.setFont(hull.getFont().deriveFont(Font.BOLD, 11f));
        hull.setForeground(SOFT_TEXT);

        art.setPreferredSize(new Dimension(CARD_WIDTH - 28, 78));

        where.setFont(where.getFont().deriveFont(Font.PLAIN, 12f));
        where.setForeground(Color.WHITE);
        condition.setFont(condition.getFont().deriveFont(Font.BOLD, 12f));

        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 82, 100)),
                BorderFactory.createEmptyBorder(8, 0, 0, 0)));

        for (JComponent c : new JComponent[]{title, hull, art, bar, where, condition, grid}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        add(title);
        add(hull);
        add(Box.createVerticalStrut(4));
        add(art);
        add(Box.createVerticalStrut(4));
        add(bar);
        add(Box.createVerticalStrut(6));
        add(where);
        add(condition);
        add(Box.createVerticalStrut(8));
        add(grid);

        // The card sits over the board, so a click on it must not fall through to a tile.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (onClose != null) onClose.run();
            }
        });
    }

    /** The colour a hull at {@code hp} of {@code max} is drawn in. */
    public static Color hullColour(int hp, int max) {
        if (hp <= 0) return SUNK;
        float share = max <= 0 ? 1f : hp / (float) max;
        if (share <= 0.25f) return CRITICAL;
        if (share <= 0.5f) return CRIPPLED;
        return HEALTHY;
    }

    /** Fills the card in for {@code sp} as it stands on {@code board} right now. */
    public void show(Ship_Placement sp, GameLayout board) {
        Ships_Type ship = sp.getShip();
        int hp = board.currentHP(sp);
        int max = board.getShipMaxHP(sp);
        boolean deployed = board.isDeployed(sp);
        accent = hullColour(hp, max);

        name.setText(ship.getName());
        hull.setText(ship.getHullClass().toUpperCase() + "  \u00b7  " + ship.getHullCode()
                + "  \u00b7  COST " + ship.getCost());
        art.setIcon(Icons.fit(ship.getImage(), CARD_WIDTH - 40, 74));
        bar.set(hp, max);

        whereText = deployed
                ? "On the board at " + span(sp) + "  \u00b7  "
                        + board.sectionsHit(sp) + " of " + ship.getSize() + " sections hit"
                : "In port" + (hp < max ? " - damage stays until it is repaired" : "");
        where.setText(wrap(whereText));

        if (hp <= 0) {
            conditionText = "Sunk - one fewer shot per salvo";
        } else if (hp <= max * 0.5f) {
            conditionText = deployed
                    ? "Crippled - the enemy can fire on its hit sections again, at half damage"
                    : "Crippled";
        } else if (hp < max) {
            conditionText = "Damaged";
        } else {
            conditionText = "Full hull";
        }
        condition.setText(wrap(conditionText));
        condition.setForeground(accent);

        ModifiedStats mods = new ModifiedStats();
        grid.removeAll();
        stat("Shields", ship.getShields(), mods.shieldModifier());
        stat("Damage", ship.getDMG(), mods.dmgModifier());
        stat("Penetration", ship.getPenetration(), 1f);
        stat("Shots", ship.getShots(), 1f);
        stat("Detection", ship.getDetection(), 1f);
        stat("Size", ship.getSize(), 1f);
        if (ship instanceof Submarine) {
            Submarine sub = (Submarine) ship;
            text("Oxygen", sub.getOxygen() + " / " + sub.getMaxOxygen());
            text("Depth", sub.isSurfaced() ? "Surfaced" : "Submerged");
        }

        setSize(getPreferredSize());
        revalidate();
        repaint();
    }

    /** The hull bar, for tests: what it is showing. */
    public HullBar getBar() {
        return bar;
    }

    /** The ship's name as shown in the title. */
    public String getTitle() {
        return name.getText();
    }

    /** The line saying where the ship is. */
    public String getWhere() {
        return whereText;
    }

    /** Wrapped rather than cut off. Swing scales CSS px by 1.3, so 195px is about 254. */
    private static String wrap(String text) {
        return "<html><div style='width:195px'>" + text + "</div></html>";
    }

    /** The line saying what shape it is in: full hull, damaged, crippled or sunk. */
    public String getCondition() {
        return conditionText;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(CARD_WIDTH, d.height);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private void stat(String label, int base, float modifier) {
        int now = Math.round(base * modifier);
        String value = String.valueOf(now);
        if (now != base) {
            int pct = Math.round((modifier - 1f) * 100f);
            value = "<html>" + now + " <font color='" + hex(BOOST) + "'>("
                    + (pct >= 0 ? "+" : "") + pct + "%)</font></html>";
        }
        text(label, value);
    }

    private void text(String label, String value) {
        JPanel cell = new JPanel(new BorderLayout(6, 0));
        cell.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(SOFT_TEXT);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setForeground(Color.WHITE);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 12f));
        cell.add(l, BorderLayout.WEST);
        cell.add(v, BorderLayout.EAST);
        grid.add(cell);
    }

    /** Where the ship lies, e.g. "B2-D2". */
    private static String span(Ship_Placement sp) {
        List<Point> tiles = sp.getOccupiedTiles();
        if (tiles.isEmpty()) return "-";
        Point a = tiles.get(0);
        Point b = tiles.get(tiles.size() - 1);
        return coord(a) + "-" + coord(b);
    }

    private static String coord(Point p) {
        return "" + (char) ('A' + p.x) + (p.y + 1);
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(new Color(0, 0, 0, 90));                       // soft drop shadow
        g2.fillRoundRect(4, 5, w - 4, h - 4, 16, 16);
        g2.setColor(BG);
        g2.fillRoundRect(0, 0, w - 4, h - 4, 16, 16);
        g2.setColor(accent);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(1, 1, w - 6, h - 6, 16, 16);
        g2.fillRoundRect(1, 1, w - 6, 5, 6, 6);                   // accent strip along the top
        g2.dispose();
        super.paintComponent(g);
    }

    /** A hull bar with the numbers written on it. */
    public static class HullBar extends JComponent {
        private int hp;
        private int max;

        HullBar() {
            setPreferredSize(new Dimension(CARD_WIDTH - 28, 20));
            setMaximumSize(new Dimension(CARD_WIDTH - 28, 20));
        }

        void set(int hp, int max) {
            this.hp = hp;
            this.max = max;
            repaint();
        }

        public int getValue() {
            return hp;
        }

        public int getMax() {
            return max;
        }

        /** What the bar reads, e.g. "HP 650 / 1300". */
        public String getText() {
            return hp <= 0 ? "SUNK" : "HP " + hp + " / " + max;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(48, 54, 66));
            g2.fillRoundRect(0, 0, w, h, 8, 8);
            float share = max <= 0 ? 0f : Math.max(0f, Math.min(1f, hp / (float) max));
            int fill = Math.round(w * share);
            if (fill > 0) {
                g2.setColor(hullColour(hp, max));
                g2.fillRoundRect(0, 0, fill, h, 8, 8);
            }
            g2.setColor(new Color(0, 0, 0, 110));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            String text = getText();
            g2.setFont(getFont() != null ? getFont().deriveFont(Font.BOLD, 12f)
                    : new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(text, tx + 1, ty + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(text, tx, ty);
            g2.dispose();
        }
    }
}
