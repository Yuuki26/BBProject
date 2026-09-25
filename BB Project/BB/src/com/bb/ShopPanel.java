package com.bb;

import Ships.Ship_Placement;
import Ships.Ships_Type;
import skills.Rarity;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The shop screen, shown between stages when {@link Shop#opensBefore(int)} says so.
 *
 * <p>Top row: what is for sale this visit - ships and one "Expand your fleet" - each with a
 * Buy and a Reroll button. Below: the player's own fleet, with a repair and a sell button on
 * each ship. Every purchase rebuilds the screen, so gold, prices and what can be afforded
 * are always current.
 */
public class ShopPanel extends JPanel {

    private static final Color CARD_BG = new Color(28, 38, 54);
    private static final Color CARD_EDGE = new Color(95, 105, 120);
    private static final Color GOLD_TEXT = new Color(255, 215, 120);
    private static final Color DAMAGED = new Color(240, 140, 120);

    private final GameLayout player;
    private final Runnable onLeave;

    private Shop shop;

    private final JLabel heading = new JLabel("Shop", SwingConstants.CENTER);
    private final JLabel subheading = new JLabel("", SwingConstants.CENTER);
    private final JPanel stockRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
    private final JPanel fleetList = new JPanel();
    private final JLabel message = new JLabel(" ", SwingConstants.CENTER);
    private final JButton repairAllButton = new JButton();
    private final JButton leaveButton = new JButton("Leave shop");

    /**
     * @param player  the board whose fleet is bought for, sold from and repaired
     * @param onLeave what happens when the player leaves - the next stage begins
     */
    public ShopPanel(GameLayout player, Runnable onLeave) {
        this.player = player;
        this.onLeave = onLeave;

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 34f));
        heading.setForeground(GOLD_TEXT);
        heading.setBorder(BorderFactory.createEmptyBorder(24, 0, 2, 0));
        subheading.setFont(subheading.getFont().deriveFont(Font.PLAIN, 16f));
        subheading.setForeground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(subheading, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        stockRow.setOpaque(false);
        fleetList.setOpaque(false);
        fleetList.setLayout(new BoxLayout(fleetList, BoxLayout.Y_AXIS));

        JScrollPane fleetScroll = new JScrollPane(fleetList);
        fleetScroll.setOpaque(false);
        fleetScroll.getViewport().setOpaque(false);
        fleetScroll.setBorder(BorderFactory.createEmptyBorder());
        fleetScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel fleetSection = new JPanel(new BorderLayout(0, 4));
        fleetSection.setOpaque(false);
        fleetSection.setBorder(BorderFactory.createEmptyBorder(0, 120, 0, 120));
        fleetSection.add(sectionLabel("Your fleet - repair or sell"), BorderLayout.NORTH);
        fleetSection.add(fleetScroll, BorderLayout.CENTER);

        JPanel stockSection = new JPanel(new BorderLayout(0, 4));
        stockSection.setOpaque(false);
        stockSection.add(sectionLabel("For sale"), BorderLayout.NORTH);
        stockSection.add(stockRow, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(stockSection, BorderLayout.NORTH);
        body.add(fleetSection, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        message.setForeground(new Color(255, 190, 140));
        message.setFont(message.getFont().deriveFont(Font.BOLD, 13f));

        repairAllButton.setFocusable(false);
        repairAllButton.addActionListener(e -> act(Shop.repairAll(player)));
        leaveButton.setFocusable(false);
        leaveButton.setFont(leaveButton.getFont().deriveFont(Font.BOLD, 14f));
        leaveButton.addActionListener(e -> {
            if (this.onLeave != null) this.onLeave.run();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4));
        buttons.setOpaque(false);
        buttons.add(repairAllButton);
        buttons.add(leaveButton);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        footer.add(message, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    /** Opens a new visit with freshly rolled stock. */
    public void open() {
        open(new Shop());
    }

    /** Opens a visit with the given stock; tests use this to control what is on sale. */
    public void open(Shop stock) {
        this.shop = stock;
        message.setText(" ");
        refresh();
    }

    /** This visit's stock. */
    public Shop getShop() {
        return shop;
    }

    // =====================================================================================
    // Building the screen
    // =====================================================================================

    private void refresh() {
        RunState run = RunState.current();
        heading.setText("Shop");
        subheading.setIcon(Icons.coin(20));
        subheading.setText(run.getCurrency() + " gold   |   Fleet cost "
                + player.getDeployedCost() + " deployed of " + run.getDeploymentBudget()
                + "   |   Next: stage " + run.getStage());

        stockRow.removeAll();
        List<Ships_Type> ships = shop.getShipsForSale();
        for (int i = 0; i < ships.size(); i++) {
            stockRow.add(shipCard(i, ships.get(i)));
        }
        stockRow.add(expansionCard());

        fleetList.removeAll();
        for (Ship_Placement sp : new ArrayList<>(player.getFleet().getPlacements())) {
            fleetList.add(fleetRow(sp));
            fleetList.add(Box.createVerticalStrut(4));
        }

        int repairAll = Shop.repairAllPrice(player);
        repairAllButton.setText(repairAll == 0 ? "Repair all" : "Repair all   " + repairAll);
        repairAllButton.setIcon(repairAll == 0 ? null : Icons.coin(16));
        repairAllButton.setEnabled(repairAll > 0 && repairAll <= run.getCurrency());
        repairAllButton.setToolTipText(repairAll == 0 ? "Nothing needs repairing"
                : "Restore every damaged ship to full hull");

        revalidate();
        repaint();
    }

    private JComponent shipCard(int index, Ships_Type ship) {
        JPanel card = card(CARD_EDGE, 2);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setOpaque(false);
        top.add(label(ship.getName(), 16f, Font.BOLD, GOLD_TEXT));
        top.add(label(ship.getHullClass() + " - cost " + ship.getCost(), 12f, Font.PLAIN,
                Color.WHITE));
        card.add(top, BorderLayout.NORTH);

        JLabel art = new JLabel(Icons.fit(ship.getImage(), 160, 70), SwingConstants.CENTER);
        card.add(art, BorderLayout.CENTER);

        int price = Shop.priceOf(ship);
        boolean sold = shop.isSold(index);
        JButton buy = priceButton(sold ? "Sold" : "Buy   " + price, !sold);
        String reason = sold ? "Already bought"
                : price > RunState.current().getCurrency() ? "Not enough gold" : null;
        buy.setEnabled(reason == null);
        buy.setToolTipText(reason == null ? "Adds " + ship.getName() + " to your roster" : reason);
        buy.addActionListener(e -> act(shop.buyShip(index, player)));

        JButton reroll = rerollButton(shop.shipRerollProblem(index),
                "Swap for another " + ship.getHullClass().toLowerCase()
                        + " - maybe this one again");
        reroll.addActionListener(e -> act(shop.rerollShip(index)));

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(label("HP " + ship.getHP() + "   DMG " + ship.getDMG()
                + "   Pen " + ship.getPenetration(), 11f, Font.PLAIN, Color.WHITE),
                BorderLayout.NORTH);
        bottom.add(buttonPair(buy, reroll), BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JComponent expansionCard() {
        Shop.Expansion ex = shop.getExpansion();
        Rarity rarity = ex.getRarity();
        JPanel card = card(rarity.getColor(), 3);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setOpaque(false);
        top.add(label("Expand your fleet", 16f, Font.BOLD, GOLD_TEXT));
        top.add(label(rarity.getLabel().toUpperCase(), 12f, Font.BOLD, rarity.getColor()));
        card.add(top, BorderLayout.NORTH);

        card.add(label("+" + ex.getAmount(), 44f, Font.BOLD, rarity.getColor()),
                BorderLayout.CENTER);

        boolean sold = shop.isExpansionSold();
        JButton buy = priceButton(sold ? "Sold" : "Buy   " + ex.getPrice(), !sold);
        buy.setEnabled(!sold && ex.getPrice() <= RunState.current().getCurrency());
        buy.setToolTipText(sold ? "Already bought this visit"
                : "Permanently adds " + ex.getAmount() + " to your fleet cost");
        buy.addActionListener(e -> act(shop.buyExpansion()));

        JButton reroll = rerollButton(shop.expansionRerollProblem(),
                "Roll the size again: 50% +2, 30% +4, 15% +6, 5% +10");
        reroll.addActionListener(e -> act(shop.rerollExpansion()));

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(label("Fleet cost +" + ex.getAmount() + ", for good", 11f, Font.PLAIN,
                Color.WHITE), BorderLayout.NORTH);
        bottom.add(buttonPair(buy, reroll), BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JComponent fleetRow(Ship_Placement sp) {
        Ships_Type ship = sp.getShip();
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(CARD_BG);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_EDGE),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel art = new JLabel(Icons.fit(ship.getImage(), 100, 34));
        art.setPreferredSize(new Dimension(104, 38));
        row.add(art, BorderLayout.WEST);

        int hp = player.currentHP(sp);
        int max = player.getShipMaxHP(sp);
        boolean damaged = hp < max;
        JLabel info = new JLabel("<html><b>" + ship.getName() + "</b> (" + ship.getHullCode()
                + ", cost " + ship.getCost() + ") - "
                + (player.isDeployed(sp) ? "deployed" : "in port") + "<br>"
                + "<font color='" + (damaged ? "#f08c78" : "#b4dcb4") + "'>HP " + hp + " / "
                + max + "</font></html>");
        info.setForeground(Color.WHITE);
        row.add(info, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.setOpaque(false);

        int repairPrice = Shop.repairPrice(player, sp);
        JButton repair = priceButton(repairPrice == 0 ? "Repair" : "Repair   " + repairPrice,
                repairPrice > 0);
        repair.setEnabled(repairPrice > 0 && repairPrice <= RunState.current().getCurrency());
        repair.setToolTipText(repairPrice == 0 ? "Not damaged" : "Restore to full hull");
        repair.addActionListener(e -> act(Shop.repair(player, sp)));

        int sellPrice = Shop.sellPriceOf(ship);
        JButton sell = priceButton("Sell   +" + sellPrice, true);
        boolean last = player.getFleet().size() <= 1;
        sell.setEnabled(!last);
        sell.setToolTipText(last ? "You can't sell your last ship"
                : "Sell for " + Shop.SELL_BACK_PERCENT + "% of its price");
        sell.addActionListener(e -> act(Shop.sell(player, sp)));

        buttons.add(repair);
        buttons.add(sell);
        row.add(buttons, BorderLayout.EAST);

        if (damaged) info.setToolTipText("Damage carries into the next battle unless repaired");
        return row;
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    /** Runs a transaction's result: shows why it was refused, and rebuilds either way. */
    private void act(String refusal) {
        if (refusal != null) {
            message.setText(refusal);
            Toolkit.getDefaultToolkit().beep();
        } else {
            message.setText(" ");
        }
        refresh();
    }

    /**
     * A reroll button at this visit's current price. {@code problem} is why this slot cannot
     * be rerolled at all (null when it can); running short of gold is checked here.
     */
    private JButton rerollButton(String problem, String tooltip) {
        int price = shop.getRerollPrice();
        JButton b = priceButton("Reroll   " + price, true);
        String reason = problem != null ? problem
                : price > RunState.current().getCurrency() ? "Not enough gold" : null;
        b.setEnabled(reason == null);
        b.setToolTipText(reason == null ? tooltip + ". Next reroll costs "
                + (price + Shop.REROLL_PRICE_STEP) + "." : reason);
        return b;
    }

    private static JPanel buttonPair(JButton top, JButton bottom) {
        JPanel pair = new JPanel(new GridLayout(2, 1, 0, 4));
        pair.setOpaque(false);
        pair.add(top);
        pair.add(bottom);
        return pair;
    }

    private JPanel card(Color edge, int thickness) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(190, 272));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(edge, thickness),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        return card;
    }

    private JButton priceButton(String text, boolean withCoin) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        if (withCoin) b.setIcon(Icons.coin(16));
        return b;
    }

    private static JLabel label(String text, float size, int style, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(style, size));
        l.setForeground(color);
        return l;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        l.setForeground(new Color(200, 215, 235));
        return l;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, 205));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
