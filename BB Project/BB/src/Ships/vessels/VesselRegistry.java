package Ships.vessels;

import Ships.Ships_Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Catalogue of every named vessel in the game.
 *
 * <p>Entries are suppliers rather than shared instances, because a hull carries per-ship
 * state - a submarine's oxygen, for one - so every placement needs its own object.
 *
 * <p>Adding a new vessel means writing its class and adding one line here; nothing else in
 * the game enumerates ship types.
 */
public final class VesselRegistry {

    /** One catalogue entry: how to build the vessel, and what it costs to field. */
    public static final class Entry {
        private final Supplier<Ships_Type> factory;
        private final int cost;
        private final String name;
        private final String hullCode;

        private Entry(Supplier<Ships_Type> factory) {
            this.factory = factory;
            Ships_Type sample = factory.get();
            this.cost = sample.getCost();
            this.name = sample.getName();
            this.hullCode = sample.getHullCode();
        }

        public Ships_Type create() {
            return factory.get();
        }

        public int getCost() {
            return cost;
        }

        public String getName() {
            return name;
        }

        public String getHullCode() {
            return hullCode;
        }

        @Override
        public String toString() {
            return name + " (" + hullCode + ", cost " + cost + ")";
        }
    }

    private static final List<Entry> ALL = new ArrayList<>();

    static {
        // Aircraft carriers
        ALL.add(new Entry(Essex::new));
        ALL.add(new Entry(Enterprise::new));
        // Battleships
        ALL.add(new Entry(Nagato::new));
        ALL.add(new Entry(Montana::new));
        ALL.add(new Entry(Monarch::new));
        // Heavy cruisers
        ALL.add(new Entry(Baltimore::new));
        ALL.add(new Entry(DesMoines::new));
        // Light cruisers
        ALL.add(new Entry(Helena::new));
        ALL.add(new Entry(Cleveland::new));
        ALL.add(new Entry(Drake::new));
        // Destroyers
        ALL.add(new Entry(I_556::new));
        ALL.add(new Entry(I_141::new));
        // Submarines
        ALL.add(new Entry(Balao::new));
        ALL.add(new Entry(Archerfish::new));

    }

    private VesselRegistry() {}

    /** Every vessel in the catalogue. */
    public static List<Entry> all() {
        return new ArrayList<>(ALL);
    }

    /** Vessels of one hull class, by its code: CV, BB, CB, CL, DD or SS. */
    public static List<Entry> byHull(String hullCode) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ALL) {
            if (e.getHullCode().equalsIgnoreCase(hullCode)) out.add(e);
        }
        return out;
    }

    /** Vessels that can be fielded for {@code budget} or less. */
    public static List<Entry> affordable(int budget) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ALL) {
            if (e.getCost() <= budget) out.add(e);
        }
        return out;
    }

    /** The cheapest vessel in the catalogue, used as a floor when budgeting a fleet. */
    public static int cheapestCost() {
        int min = Integer.MAX_VALUE;
        for (Entry e : ALL) {
            min = Math.min(min, e.getCost());
        }
        return min == Integer.MAX_VALUE ? 1 : min;
    }
}
