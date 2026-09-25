package com.bb;

import Ships.Ship_Placement;
import Ships.Ships_Type;
import Ships.vessels.VesselRegistry;
import skills.Rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The shop between stages: the only place to get more ships.
 *
 * <p>It opens before stage {@value #FIRST_STAGE} and then every {@value #EVERY} stages after
 * (4, 7, 10, ...). A visit offers:
 * <ul>
 *   <li>a few ships for the roster;</li>
 *   <li>"Expand your fleet" - permanent extra fleet cost, of a rarity rolled per visit;</li>
 *   <li>selling ships back for {@value #SELL_BACK_PERCENT}% of their price;</li>
 *   <li>repairing one damaged ship, or all of them;</li>
 *   <li>rerolling any unsold offer, for {@value #REROLL_BASE_PRICE} gold and then
 *       {@value #REROLL_PRICE_STEP} more each time in the same visit.</li>
 * </ul>
 *
 * <p>A reroll keeps what kind of offer the slot is: a light cruiser rerolls into a light
 * cruiser. It can land on the very ship it replaced, or on one another slot already shows.
 * "Expand your fleet" is the exception - rerolling it rolls its rarity again from scratch,
 * since the rarity is all there is to it.
 *
 * <p>One {@code Shop} object is one visit: its stock is rolled when it is created, and each
 * offer can be bought once. Prices follow each vessel's own cost, so repricing a ship's
 * {@code .cost(...)} reprices it here too.
 *
 * <p>Every transaction returns {@code null} when it went through, or a sentence saying why
 * not, so the screen can show it as is.
 */
public class Shop {

    /** First stage the shop opens before, and how many stages apart it opens after that. */
    public static final int FIRST_STAGE = 4;
    public static final int EVERY = 3;

    /** Gold per point of a ship's cost: a cost-10 battleship sells for 120. */
    public static final int PRICE_PER_COST = 12;

    /** Share of a ship's price paid back when it is sold. */
    public static final int SELL_BACK_PERCENT = 60;

    /** Gold per point of fleet cost bought with "Expand your fleet". */
    public static final int EXPANSION_PRICE_PER_COST = 15;

    /**
     * Repairing a ship from nothing to full costs this share of its price; lighter damage
     * costs proportionally less.
     */
    public static final double REPAIR_SHARE = 0.5;

    /** Ships offered per visit. */
    public static final int SHIPS_ON_SALE = 4;

    /** The first reroll in a visit costs this, and each one after it costs this much more. */
    public static final int REROLL_BASE_PRICE = 5;
    public static final int REROLL_PRICE_STEP = 5;

    /** The four sizes "Expand your fleet" comes in, with the chance of each per visit. */
    public enum Expansion {
        UNCOMMON(Rarity.UNCOMMON, 2, 50),
        RARE(Rarity.RARE, 4, 30),
        EPIC(Rarity.EPIC, 6, 15),
        LEGENDARY(Rarity.LEGENDARY, 10, 5);

        private final Rarity rarity;
        private final int amount;
        private final int chancePercent;

        Expansion(Rarity rarity, int amount, int chancePercent) {
            this.rarity = rarity;
            this.amount = amount;
            this.chancePercent = chancePercent;
        }

        public Rarity getRarity() { return rarity; }

        /** Fleet cost added. */
        public int getAmount() { return amount; }

        /** Chance, out of 100, that a visit offers this size. */
        public int getChancePercent() { return chancePercent; }

        public int getPrice() { return amount * EXPANSION_PRICE_PER_COST; }
    }

    private final List<Ships_Type> forSale = new ArrayList<>();
    private final boolean[] sold;
    private Expansion expansion;
    private boolean expansionSold;

    private final Random rand;
    private int rerolls;   // this visit, which sets the next reroll's price

    /** Rolls a fresh visit's stock. */
    public Shop() {
        this(new Random());
    }

    public Shop(Random rand) {
        this.rand = rand;
        // Only ships the player could ever field: one that costs more than the whole fleet
        // budget would sit in port forever.
        int budget = RunState.current().getDeploymentBudget();
        List<VesselRegistry.Entry> catalogue = VesselRegistry.affordable(budget);
        Collections.shuffle(catalogue, rand);
        for (int i = 0; i < Math.min(SHIPS_ON_SALE, catalogue.size()); i++) {
            forSale.add(catalogue.get(i).create());
        }
        sold = new boolean[forSale.size()];
        expansion = rollExpansion(rand);
    }

    // =====================================================================================
    // Rules
    // =====================================================================================

    /** True when the shop opens before {@code stage}: 4, 7, 10, ... */
    public static boolean opensBefore(int stage) {
        return stage >= FIRST_STAGE && (stage - FIRST_STAGE) % EVERY == 0;
    }

    public static int priceOf(Ships_Type ship) {
        return ship.getCost() * PRICE_PER_COST;
    }

    public static int sellPriceOf(Ships_Type ship) {
        return priceOf(ship) * SELL_BACK_PERCENT / 100;
    }

    /** Gold to bring {@code sp} back to full hull; 0 when it is not damaged. */
    public static int repairPrice(GameLayout player, Ship_Placement sp) {
        int max = player.getShipMaxHP(sp);
        int missing = max - player.currentHP(sp);
        if (max <= 0 || missing <= 0) return 0;
        return Math.max(1, (int) Math.ceil(priceOf(sp.getShip()) * REPAIR_SHARE * missing / max));
    }

    /** Gold to repair every damaged ship the player owns. */
    public static int repairAllPrice(GameLayout player) {
        int sum = 0;
        for (Ship_Placement sp : player.getFleet().getPlacements()) {
            sum += repairPrice(player, sp);
        }
        return sum;
    }

    /** Picks this visit's expansion size: 50% uncommon, 30% rare, 15% epic, 5% legendary. */
    public static Expansion rollExpansion(Random rand) {
        int ticket = rand.nextInt(100);
        for (Expansion e : Expansion.values()) {
            ticket -= e.getChancePercent();
            if (ticket < 0) return e;
        }
        return Expansion.UNCOMMON;
    }

    // =====================================================================================
    // Stock
    // =====================================================================================

    public List<Ships_Type> getShipsForSale() {
        return Collections.unmodifiableList(forSale);
    }

    public boolean isSold(int index) {
        return sold[index];
    }

    public Expansion getExpansion() {
        return expansion;
    }

    public boolean isExpansionSold() {
        return expansionSold;
    }

    // =====================================================================================
    // Rerolls
    // =====================================================================================

    /** What the next reroll costs: 5, then 10, then 15, and so on, for this visit. */
    public int getRerollPrice() {
        return REROLL_BASE_PRICE + REROLL_PRICE_STEP * rerolls;
    }

    /** Rerolls bought so far this visit. */
    public int getRerollCount() {
        return rerolls;
    }

    /**
     * The ships the slot at {@code index} can reroll into: every vessel of the same hull
     * class that fits the fleet cost - including the one there now.
     */
    public List<VesselRegistry.Entry> rerollChoices(int index) {
        List<VesselRegistry.Entry> out = new ArrayList<>();
        if (index < 0 || index >= forSale.size()) return out;
        int budget = RunState.current().getDeploymentBudget();
        for (VesselRegistry.Entry e : VesselRegistry.byHull(forSale.get(index).getHullCode())) {
            if (e.getCost() <= budget) out.add(e);
        }
        return out;
    }

    /**
     * Why the ship at {@code index} cannot be rerolled, not counting gold; null when it can.
     * A slot whose hull class has nothing else that fits is refused rather than charged for
     * a reroll that could only hand back the same ship.
     */
    public String shipRerollProblem(int index) {
        if (index < 0 || index >= forSale.size()) return "That ship is not for sale.";
        if (sold[index]) return "You already bought that one.";
        Ships_Type ship = forSale.get(index);
        List<VesselRegistry.Entry> choices = rerollChoices(index);
        if (choices.isEmpty()
                || choices.size() == 1 && choices.get(0).getName().equals(ship.getName())) {
            return "No other " + ship.getHullClass().toLowerCase() + " fits your fleet cost.";
        }
        return null;
    }

    /** Why the expansion cannot be rerolled, not counting gold; null when it can. */
    public String expansionRerollProblem() {
        return expansionSold ? "You already expanded your fleet this visit." : null;
    }

    /** Rerolls the ship at {@code index} into another of its hull class. */
    public String rerollShip(int index) {
        String problem = shipRerollProblem(index);
        if (problem != null) return problem;
        if (!payForReroll()) return "Not enough gold to reroll.";

        List<VesselRegistry.Entry> choices = rerollChoices(index);
        forSale.set(index, choices.get(rand.nextInt(choices.size())).create());
        return null;
    }

    /** Rolls the expansion's rarity again, with the usual 50 / 30 / 15 / 5 odds. */
    public String rerollExpansion() {
        String problem = expansionRerollProblem();
        if (problem != null) return problem;
        if (!payForReroll()) return "Not enough gold to reroll.";

        expansion = rollExpansion(rand);
        return null;
    }

    private boolean payForReroll() {
        if (!RunState.current().spendCurrency(getRerollPrice())) return false;
        rerolls++;
        return true;
    }

    // =====================================================================================
    // Transactions
    // =====================================================================================

    /** Buys the ship at {@code index} into the roster, undeployed. */
    public String buyShip(int index, GameLayout player) {
        if (index < 0 || index >= forSale.size()) return "That ship is not for sale.";
        if (sold[index]) return "You already bought that one.";

        Ships_Type ship = forSale.get(index);
        int budget = RunState.current().getDeploymentBudget();
        if (ship.getCost() > budget) {
            return ship.getName() + " costs " + ship.getCost()
                    + " to field - more than your whole fleet cost of " + budget + ".";
        }
        if (!RunState.current().spendCurrency(priceOf(ship))) {
            return "Not enough gold for " + ship.getName() + ".";
        }

        player.addToRoster(ship);
        sold[index] = true;
        return null;
    }

    /** Buys this visit's fleet expansion. */
    public String buyExpansion() {
        if (expansionSold) return "You already expanded your fleet this visit.";
        if (!RunState.current().spendCurrency(expansion.getPrice())) {
            return "Not enough gold to expand your fleet.";
        }
        RunState.current().addBudgetBonus(expansion.getAmount());
        expansionSold = true;
        return null;
    }

    /** Sells {@code sp} for {@value #SELL_BACK_PERCENT}% of its price. */
    public static String sell(GameLayout player, Ship_Placement sp) {
        if (sp == null || !player.getFleet().getPlacements().contains(sp)) {
            return "That ship is not in your fleet.";
        }
        if (player.getFleet().size() <= 1) {
            return "You can't sell your last ship.";
        }
        RunState.current().addCurrency(sellPriceOf(sp.getShip()));
        player.sellShip(sp);
        return null;
    }

    /** Repairs one ship to full hull. */
    public static String repair(GameLayout player, Ship_Placement sp) {
        int price = repairPrice(player, sp);
        if (price == 0) return sp.getShip().getName() + " is not damaged.";
        if (!RunState.current().spendCurrency(price)) {
            return "Not enough gold to repair " + sp.getShip().getName() + ".";
        }
        player.repairShip(sp);
        return null;
    }

    /** Repairs every damaged ship at once. */
    public static String repairAll(GameLayout player) {
        int price = repairAllPrice(player);
        if (price == 0) return "Nothing needs repairing.";
        if (!RunState.current().spendCurrency(price)) {
            return "Not enough gold to repair the whole fleet.";
        }
        player.repairAll();
        return null;
    }
}
