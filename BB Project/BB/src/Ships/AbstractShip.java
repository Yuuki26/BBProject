package Ships;

/**
 * Shared plumbing for every hull.
 *
 * <p>Holds the stat block and the vessel's name so the six hull classes only have to declare
 * what makes them different: their artwork and any unique mechanic such as a submarine's
 * oxygen.
 *
 * <p>Stats are read-only after construction. A named vessel sets them once by passing a
 * {@link ShipStats}; nothing later reaches in and edits a hull's numbers.
 */
public abstract class AbstractShip implements Ships_Type {

    private final String name;
    private final ShipStats stats;

    /**
     * Copied out of the stat block when the ship is built, so nothing that later gets hold
     * of the {@link ShipStats} - a skill, a reward, a subclass - can reprice the ship.
     */
    private final int cost;

    protected AbstractShip(String name, ShipStats stats) {
        this.name = name;
        this.stats = stats == null ? new ShipStats() : stats;
        this.cost = this.stats.cost();

        // A forgotten .cost(...) would otherwise make the ship free, and a free ship can be
        // deployed without limit - which quietly breaks the budget the difficulty rests on.
        if (cost < 1) {
            throw new IllegalStateException(name + " has no cost: every vessel must set "
                    + ".cost(...) in its ShipStats, and it must be at least 1.");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * What this vessel costs to deploy. Set per vessel in its {@link ShipStats}, fixed at
     * construction, and final here so no subclass can override it.
     */
    @Override
    public final int getCost() {
        return cost;
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

    // ---- artwork -------------------------------------------------------------------------

    /** Resolved on first use: the vessel's own picture if its file exists, else the hull's. */
    private String resolvedImage;

    /**
     * The picture to show for this ship.
     *
     * <p>Each vessel names its own file with {@link ShipStats#image(String)}. Until that file
     * is actually in {@code lib}, the hull class's picture stands in, so a ship can be given
     * its art later just by dropping the file in - no code change.
     */
    @Override
    public final String getImage() {
        if (resolvedImage == null) {
            String own = stats.image();
            resolvedImage = own != null && com.bb.Assets.getResource(own) != null
                    ? own : defaultImage();
        }
        return resolvedImage;
    }

    /** The picture this vessel asks for, whether or not the file exists yet. */
    public String getVesselImage() {
        return stats.image();
    }

    /** The hull class's picture, used until the vessel has one of its own. */
    protected abstract String defaultImage();

    @Override
    public String toString() {
        return getName() + " (" + getHullCode() + ", cost " + getCost() + ")";
    }
}
