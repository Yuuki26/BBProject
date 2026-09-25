package com.bb;

import skills.Rarity;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The between-stages screen: shows what the player just earned and offers a pick.
 *
 * <p>This is the hinge that turns a single win into a run. Clearing a stage lands here
 * instead of on the end screen, and choosing a card carries the fleet into the next battle.
 *
 * <p>Each screen has one free reroll, spent on whichever card the player likes. It swaps that
 * card for another of the same kind and rarity (see {@link Reward#reroll}), and nothing shown
 * on this screen - rerolled away or still on the table - can be what it rolls into. The next
 * screen starts over, with its own reroll and a clean slate.
 */
public class RewardPanel extends JPanel {

    private static final int CARDS = 3;

    /** Free rerolls per reward screen. */
    public static final int REROLLS_PER_SCREEN = 1;

    private final JLabel heading = new JLabel("", SwingConstants.CENTER);
    private final JLabel subheading = new JLabel("", SwingConstants.CENTER);
    private final JPanel cardRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 18));
    private final JLabel rerollHint = new JLabel("", SwingConstants.CENTER);
    private final Consumer<Reward> onPicked;
    private final Random rand = new Random();

    private GameLayout player;
    private final List<Reward> options = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();   // every offer shown on this screen
    private int rerollsLeft;

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

        rerollHint.setForeground(new Color(190, 205, 225));
        rerollHint.setFont(rerollHint.getFont().deriveFont(Font.PLAIN, 13f));
        rerollHint.setVerticalAlignment(SwingConstants.TOP);

        // The hint sits right under the cards rather than at the foot of the window.
        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(false);
        middle.add(cardRow, BorderLayout.NORTH);
        middle.add(rerollHint, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
    }

    /** Rebuilds the screen for a freshly cleared stage. */
    public void present(GameLayout player, int goldEarned) {
        RunState run = RunState.current();

        // The run has already moved on to the next stage by the time this screen is shown,
        // so the one just cleared is the stage before it.
        heading.setText("Stage " + Math.max(1, run.getStage() - 1) + " cleared");
        subheading.setIcon(Icons.coin(20));
        subheading.setText("+" + goldEarned + " gold (" + run.getCurrency() + ")"
                + "   |   Score " + run.getScore()
                + "   |   Salvo size " + player.getAvailableShots()
                + "   |   Choose one upgrade to carry forward");

        this.player = player;
        options.clear();
        options.addAll(Reward.roll(player, CARDS));
        seen.clear();
        for (Reward r : options) seen.add(r.key());
        rerollsLeft = REROLLS_PER_SCREEN;
        rebuildCards();
    }

    /** The cards on the table right now. */
    public List<Reward> getOptions() {
        return new ArrayList<>(options);
    }

    /** Free rerolls still to spend on this screen. */
    public int getRerollsLeft() {
        return rerollsLeft;
    }

    /**
     * Rerolls the card at {@code index}.
     *
     * @return null when it went through, or why not
     */
    public String reroll(int index) {
        if (index < 0 || index >= options.size()) return "There is no card there.";
        if (rerollsLeft <= 0) return "You have already used this screen's reroll.";

        Reward current = options.get(index);
        Reward replacement = Reward.reroll(current, player, seen, rand);
        if (replacement == null) return nothingElseLike(current);

        options.set(index, replacement);
        seen.add(replacement.key());
        rerollsLeft--;
        rebuildCards();
        return null;
    }

    private void rebuildCards() {
        cardRow.removeAll();
        for (int i = 0; i < options.size(); i++) {
            cardRow.add(buildCard(i, options.get(i)));
        }
        rerollHint.setText(rerollsLeft > 0
                ? "One free reroll on this screen. It keeps the card's kind and rarity, "
                        + "and won't bring back anything already shown here."
                : "Reroll used. The next reward screen gets a fresh one.");
        revalidate();
        repaint();
    }

    private static String nothingElseLike(Reward r) {
        return r.getRarity() == null
                ? "There is nothing else like " + r.getTitle() + " to roll into."
                : "No other " + r.getRarity().getLabel() + " skill is left to roll into.";
    }

    private JComponent buildCard(int index, Reward reward) {
        // Skill cards take their rarity's colour, so a rare pick stands out before it is read.
        // Everything else gets a neutral slate edge, which no rarity colour is close to.
        Rarity rarity = reward.getRarity();
        Color edge = rarity != null ? rarity.getColor() : new Color(95, 105, 120);

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setPreferredSize(new Dimension(260, 340));
        card.setBackground(new Color(28, 38, 54));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(edge, rarity != null ? 3 : 2),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel title = new JLabel(reward.getTitle(), SwingConstants.CENTER);
        title.setForeground(new Color(255, 215, 120));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        if (rarity != null) {
            JLabel tier = new JLabel(rarity.getLabel().toUpperCase()
                    + (reward.isUpgrade() ? "  \u00b7  UPGRADE" : ""), SwingConstants.CENTER);
            tier.setForeground(rarity.getColor());
            tier.setFont(tier.getFont().deriveFont(Font.BOLD, 12f));
            top.add(tier, BorderLayout.CENTER);
        }
        card.add(top, BorderLayout.NORTH);

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

        // HTML so long descriptions wrap inside the card instead of being clipped. Swing's HTML
        // renderer scales CSS px by 1.3, so 165px lays out about 215 real pixels wide - inside
        // the card's ~226. The old 210px came out near 273 and cut the ends off every line.
        JLabel desc = new JLabel("<html><div style='text-align:center;width:165px'>"
                + escape(reward.getDescription()) + "</div></html>", SwingConstants.CENTER);
        desc.setForeground(Color.WHITE);
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 12f));
        bottom.add(desc, BorderLayout.CENTER);

        JButton take = new JButton("Take");
        take.setFocusable(false);
        take.addActionListener(e -> {
            if (onPicked != null) onPicked.accept(reward);
        });

        String blocked = rerollsLeft <= 0 ? "You have already used this screen's reroll."
                : !Reward.canReroll(reward, player, seen) ? nothingElseLike(reward) : null;
        JButton reroll = new JButton(rerollsLeft <= 0 ? "Reroll used"
                : blocked != null ? "No reroll" : "Reroll (free)");
        reroll.setFocusable(false);
        reroll.setEnabled(blocked == null);
        reroll.setToolTipText(blocked != null ? blocked
                : reward.getRarity() != null
                        ? "Swap for another " + reward.getRarity().getLabel() + " skill"
                        : "Swap this card");
        reroll.addActionListener(e -> {
            if (reroll(index) != null) Toolkit.getDefaultToolkit().beep();
        });

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(reroll);
        buttons.add(take);
        bottom.add(buttons, BorderLayout.SOUTH);

        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private String symbolFor(Reward.Kind kind) {
        switch (kind) {
            case SHOTS:  return "++";
            case REPAIR: return "+";
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
