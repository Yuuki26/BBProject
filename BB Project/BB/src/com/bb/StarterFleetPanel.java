package com.bb;

import Ships.Ships_Type;
import Ships.vessels.VesselRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Where a run's fleet comes from: three ships, dealt like reward cards.
 *
 * <p>A new run deals {@value #STARTER_SHIPS} different vessels, and those are the starter
 * fleet. Each one costs no more than the stage-1 fleet cost on its own, so any of them can
 * be deployed - but together they may cost more (8 + 4 + 6, say), in which case the player
 * picks which to put on the board and the rest wait in port.
 *
 * <p>Every ship can be rerolled {@value #REROLLS_PER_SHIP} time. A reroll keeps the hull
 * class - a destroyer rerolls into another destroyer - and never lands on a vessel this
 * screen has already shown, whether it is still on the table or was rerolled away. When
 * nothing of that class is left that fits, the reroll is off for that ship.
 *
 * <p>After this, the shop is the only way to get more ships.
 */
public class StarterFleetPanel extends JPanel {

    /** Ships in a starter fleet. */
    public static final int STARTER_SHIPS = 3;

    /** Rerolls each starter ship gets. */
    public static final int REROLLS_PER_SHIP = 1;

    private static final Color CARD_BG = new Color(28, 38, 54);
    private static final Color CARD_EDGE = new Color(95, 105, 120);
    private static final Color GOLD_TEXT = new Color(255, 215, 120);

    private final GameLayout player;
    private final Runnable onDone;

    private Random rand = new Random();
    private final List<VesselRegistry.Entry> offers = new ArrayList<>();
    private final List<Integer> rerollsLeft = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();   // every vessel shown this deal

    private final JLabel costLine = new JLabel("", SwingConstants.CENTER);
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final JButton confirm = new JButton("Set sail");
    private final JPanel cardRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 18));

    /**
     * @param player the board the starter fleet is installed on
     * @param onDone what happens once the fleet is confirmed
     */
    public StarterFleetPanel(GameLayout player, Runnable onDone) {
        this.player = player;
        this.onDone = onDone;

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        JLabel title = new JLabel("Your starter fleet", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        title.setForeground(GOLD_TEXT);
        title.setBorder(BorderFactory.createEmptyBorder(36, 0, 2, 0));

        costLine.setFont(costLine.getFont().deriveFont(Font.PLAIN, 16f));
        costLine.setForeground(Color.WHITE);

        JLabel hint = new JLabel("These three are the whole fleet you start with - more can "
                + "only be bought in the shop. Each ship can be rerolled once.",
                SwingConstants.CENTER);
        hint.setForeground(new Color(190, 205, 225));

        JPanel header = new JPanel(new GridLayout(3, 1));
        header.setOpaque(false);
        header.add(title);
        header.add(costLine);
        header.add(hint);
        add(header, BorderLayout.NORTH);

        cardRow.setOpaque(false);
        add(cardRow, BorderLayout.CENTER);

        message.setForeground(new Color(255, 190, 140));
        message.setFont(message.getFont().deriveFont(Font.BOLD, 13f));
        confirm.setFocusable(false);
        confirm.setFont(confirm.getFont().deriveFont(Font.BOLD, 15f));
        confirm.setPreferredSize(new Dimension(200, 40));
        confirm.addActionListener(e -> onConfirm());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 40, 0));
        footer.add(message, BorderLayout.NORTH);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonRow.setOpaque(false);
        buttonRow.add(confirm);
        footer.add(buttonRow, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        reset();
    }

    /** Deals a fresh starter fleet, for a new run. */
    public void reset() {
        reset(new Random());
    }

    /** Deals a fresh starter fleet from {@code rand}, which the rerolls then draw from too. */
    public void reset(Random rand) {
        this.rand = rand;
        offers.clear();
        rerollsLeft.clear();
        seen.clear();

        List<VesselRegistry.Entry> fits = VesselRegistry.affordable(budget());
        Collections.shuffle(fits, rand);
        for (int i = 0; i < Math.min(STARTER_SHIPS, fits.size()); i++) {
            offers.add(fits.get(i));
            rerollsLeft.add(REROLLS_PER_SHIP);
            seen.add(fits.get(i).getName());
        }
        message.setText(" ");
        rebuild();
    }

    /** The ships dealt, in card order. */
    public List<VesselRegistry.Entry> getOffers() {
        return new ArrayList<>(offers);
    }

    /** Rerolls left on the ship at {@code index}. */
    public int getRerollsLeft(int index) {
        return rerollsLeft.get(index);
    }

    /**
     * What the ship at {@code index} could reroll into: same hull class, fits the fleet cost,
     * and not shown on this screen before.
     */
    public List<VesselRegistry.Entry> rerollChoices(int index) {
        List<VesselRegistry.Entry> out = new ArrayList<>();
        int budget = budget();
        for (VesselRegistry.Entry e : VesselRegistry.byHull(offers.get(index).getHullCode())) {
            if (e.getCost() <= budget && !seen.contains(e.getName())) out.add(e);
        }
        return out;
    }

    /**
     * Rerolls the ship at {@code index}.
     *
     * @return null when it went through, or why not
     */
    public String reroll(int index) {
        if (index < 0 || index >= offers.size()) return "There is no ship there.";
        String problem = rerollProblem(index);
        if (problem != null) return problem;

        List<VesselRegistry.Entry> choices = rerollChoices(index);
        VesselRegistry.Entry next = choices.get(rand.nextInt(choices.size()));
        offers.set(index, next);
        seen.add(next.getName());
        rerollsLeft.set(index, rerollsLeft.get(index) - 1);
        message.setText(" ");
        rebuild();
        return null;
    }

    private String rerollProblem(int index) {
        if (rerollsLeft.get(index) <= 0) {
            return "You have already rerolled " + offers.get(index).getName() + "'s slot.";
        }
        if (rerollChoices(index).isEmpty()) {
            return "No other " + offers.get(index).create().getHullClass().toLowerCase()
                    + " fits your fleet cost.";
        }
        return null;
    }

    private int budget() {
        return RunState.current().getDeploymentBudget();
    }

    // =====================================================================================
    // Building the screen
    // =====================================================================================

    private void rebuild() {
        int total = 0;
        for (VesselRegistry.Entry e : offers) total += e.getCost();
        int budget = budget();
        costLine.setText("Fleet cost " + budget + "   |   these ships cost " + total
                + " together" + (total <= budget ? " - all of them fit on the board"
                        : " - deploy what fits, the rest wait in port"));
        confirm.setEnabled(!offers.isEmpty());

        cardRow.removeAll();
        for (int i = 0; i < offers.size(); i++) {
            cardRow.add(shipCard(i, offers.get(i)));
        }
        revalidate();
        repaint();
    }

    private JComponent shipCard(int index, VesselRegistry.Entry entry) {
        Ships_Type ship = entry.create();

        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setPreferredSize(new Dimension(260, 340));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_EDGE, 2),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel name = new JLabel(ship.getName(), SwingConstants.CENTER);
        name.setForeground(GOLD_TEXT);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 17f));
        JLabel hull = new JLabel(ship.getHullClass().toUpperCase() + "  \u00b7  COST "
                + ship.getCost(), SwingConstants.CENTER);
        hull.setForeground(new Color(200, 215, 235));
        hull.setFont(hull.getFont().deriveFont(Font.BOLD, 12f));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(name, BorderLayout.NORTH);
        top.add(hull, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        card.add(new JLabel(Icons.fit(ship.getImage(), 220, 110), SwingConstants.CENTER),
                BorderLayout.CENTER);

        JLabel stats = new JLabel("<html><div style='text-align:center;width:165px'>"
                + "HP " + ship.getHP() + " &nbsp; DMG " + ship.getDMG()
                + " &nbsp; Pen " + ship.getPenetration() + "<br>"
                + "Shield " + ship.getShields() + " &nbsp; Size " + ship.getSize()
                + " &nbsp; Detection " + ship.getDetection() + "</div></html>",
                SwingConstants.CENTER);
        stats.setForeground(Color.WHITE);
        stats.setFont(stats.getFont().deriveFont(Font.PLAIN, 12f));

        int left = rerollsLeft.get(index);
        String blocked = rerollProblem(index);
        JButton reroll = new JButton(left <= 0 ? "Rerolled"
                : blocked != null ? "No other " + ship.getHullClass().toLowerCase()
                : "Reroll (" + left + " left)");
        reroll.setFocusable(false);
        reroll.setEnabled(blocked == null);
        reroll.setToolTipText(blocked != null ? blocked
                : "Swap for another " + ship.getHullClass().toLowerCase());
        reroll.addActionListener(e -> {
            String refusal = reroll(index);
            if (refusal != null) {
                message.setText(refusal);
                Toolkit.getDefaultToolkit().beep();
            }
        });

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(stats, BorderLayout.CENTER);
        bottom.add(reroll, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void onConfirm() {
        List<Ships_Type> ships = new ArrayList<>();
        for (VesselRegistry.Entry e : offers) ships.add(e.create());

        String refusal = player.setStarterFleet(ships);
        if (refusal != null) {
            message.setText(refusal);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        if (onDone != null) onDone.run();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 205));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
