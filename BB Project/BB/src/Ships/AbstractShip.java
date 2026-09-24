package Ships;

/**
 * Shared plumbing for every hull.
 *
 * <p>Holds the stat block and the vessel's name so the six hull classes only have to declare
 * what makes them different: their fixed {@link #getCost() cost}, their artwork, and any
 * unique mechanic such as a submarine's oxygen.
 *
 * <p>Stats are read-only after construction. A named vessel sets them once by passing a
 * {@link ShipStats}; nothing later reaches in and edits a hull's numbers, so two ships of the
 * same class always behave the same way.
 */
public abstract class AbstractShip implements Ships_Type {

    private final String name;
    private final ShipStats stats;

    protected AbstractShip(String name, ShipStats stats) {
        this.name = name;
        this.stats = stats == null ? new ShipStats() : stats;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getHP() {
        return stats.hp();
    }

    @Override
    public int getShields() {
        return stats.shields();
    }

    @Override
    public int getDMG() {
        return stats.dmg();
    }

    @Override
    public int getShots() {
        return stats.shots();
    }

    @Override
    public int getPenetration() {
        return stats.penetration();
    }

    @Override
    public int getDetection() {
        return stats.detection();
    }

    @Override
    public int getSize() {
        return stats.size();
    }

    /** The stat block, for subclasses that derive a value from a base number. */
    protected ShipStats stats() {
        return stats;
    }

    @Override
    public String toString() {
        return getName() + " (" + getHullCode() + ", cost " + getCost() + ")";
    }
}
