package com.bb;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * The between-stages screen: shows what the player just earned and offers a pick.
 *
 * <p>This is the hinge that turns a single win into a run. Clearing a stage lands here
 * instead of on the end screen, and choosing a card carries the fleet into the next battle.
 */
public class RewardPanel extends JPanel {

    private static final int CARDS = 3;

    private final JLabel heading = new JLabel("", SwingConstants.CENTER);
    private final JLabel subheading = new JLabel("", SwingConstants.CENTER);
    private final JPanel cardRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 18));
    private final Consumer<Reward> onPicked;

    public RewardPanel(Consumer<Reward> onPicked) {
        this.onPicked = onPicked;

        setLayout(new BorderLayout());
        setOpaque(false);

        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 34f));
        heading.setForeground(new Color(255, 215, 120));
        heading.setBorder(BorderFactory.createEmptyBorder(40, 0, 6, 0));

        subheading.setFont(subheading.getFont().deriveFont(Font.PLAIN, 16f));
        subheading.setForeground(Color.WHITE);
        subheading.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(subheading, BorderLayout.CENTER);

        cardRow.setOpaque(false);

        add(header, BorderLayout.NORTH);
        add(cardRow, BorderLayout.CENTER);
    }

    /** Rebuilds the screen for a freshly cleared stage. */
    public void present(GameLayout player) {
        RunState run = RunState.current();

        heading.setText("Stage " + run.getStage() + " cleared");
        subheading.setText("Score " + run.getScore()
                + "   |   Salvo size " + player.getAvailableShots()
                + "   |   Choose one upgrade to carry forward");

        cardRow.removeAll();
        List<Reward> options = Reward.roll(player, CARDS);
        for (Reward r : options) {
            cardRow.add(buildCard(r));
        }
        revalidate();
        repaint();
    }

    private JComponent buildCard(Reward reward) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setPreferredSize(new Dimension(260, 340));
        card.setBackground(new Color(28, 38, 54));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 130, 190), 2),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel title = new JLabel(reward.getTitle(), SwingConstants.CENTER);
        title.setForeground(new Color(255, 215, 120));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        card.add(title, BorderLayout.NORTH);

        JLabel art = new JLabel("", SwingConstants.CENTER);
        ImageIcon icon = loadIcon(reward.getIconPath(), 150, 150);
        if (icon != null) {
            art.setIcon(icon);
        } else {
            art.setText(symbolFor(reward.getKind()));
            art.setFont(art.getFont().deriveFont(Font.BOLD, 64f));
            art.setForeground(new Color(140, 190, 240));
        }
        card.add(art, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);

        // HTML so long descriptions wrap inside the card instead of being clipped.
        JLabel desc = new JLabel("<html><div style='text-align:center;width:210px'>"
                + escape(reward.getDescription()) + "</div></html>", SwingConstants.CENTER);
        desc.setForeground(Color.WHITE);
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 12f));
        bottom.add(desc, BorderLayout.CENTER);

        JButton take = new JButton("Take");
        take.setFocusable(false);
        take.addActionListener(e -> {
            if (onPicked != null) onPicked.accept(reward);
        });
        bottom.add(take, BorderLayout.SOUTH);

        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private String symbolFor(Reward.Kind kind) {
        switch (kind) {
            case SHOTS:  return "++";
            case REPAIR: return "+";
            case SHIP:   return "#";
            default:     return "*";
        }
    }

    private ImageIcon loadIcon(String path, int w, int h) {
        if (path == null) return null;
        String resource = path.startsWith("/") ? path : "/" + path;
        java.net.URL url = Assets.getResource(resource);
        if (url == null) return null;
        Image scaled = new ImageIcon(url).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 205));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
