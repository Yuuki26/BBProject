package Ships;

/**
 * Submarine (SS): nearly invisible while submerged, and on a clock.
 *
 * <h2>Oxygen</h2>
 * A submerged boat spends one oxygen every turn. At zero it is forced to the surface, and a
 * surfaced boat is far easier to find - {@link #getDetection()} rises by
 * {@link #SURFACED_DETECTION_PENALTY} the moment it breaks the surface, whether that was the
 * player's choice or the air running out.
 *
 * <p>Surfacing is also how the air comes back: a boat on the surface recovers
 * {@link #OXYGEN_REGEN_PER_TURN} per turn and can dive again once it has enough to be worth
 * the trip. That refill rule is the one part of the mechanic not dictated by the spec - it
 * exists so running out is a setback rather than a permanent loss of the hull's whole point.
 */
public abstract class Submarine extends AbstractShip {

    /** Fixed build cost for every submarine. */
    public static final int COST = 3;

    /** Added to detection while the boat is on the surface. */
    public static final int SURFACED_DETECTION_PENALTY = 4;

    /** Oxygen recovered per turn while surfaced. */
    public static final int OXYGEN_REGEN_PER_TURN = 2;

    /** Oxygen needed before the boat is willing to dive again. */
    public static final int OXYGEN_TO_DIVE = 2;

    private final int maxOxygen;
    private int oxygen;
    private boolean surfaced;

    protected Submarine(String name, ShipStats stats, int maxOxygen) {
        super(name, stats);
        this.maxOxygen = Math.max(1, maxOxygen);
        this.oxygen = this.maxOxygen;
        this.surfaced = false;
    }

    @Override
    public final int getCost() {
        return COST;
    }

    @Override
    public String getHullClass() {
        return "Submarine";
    }

    @Override
    public String getHullCode() {
        return "SS";
    }

    @Override
    public String getImage() {
        return "/ships/submarine.png";
    }

    // =====================================================================================
    // Oxygen
    // =====================================================================================

    public int getMaxOxygen() {
        return maxOxygen;
    }

    public int getOxygen() {
        return oxygen;
    }

    public boolean isSurfaced() {
        return surfaced;
    }

    /**
     * Detection, raised while the boat is on the surface.
     *
     * <p>This is the whole point of the oxygen clock: a submerged boat is the hardest thing
     * on the board to find, and a surfaced one is not.
     */
    @Override
    public int getDetection() {
        int base = super.getDetection();
        return surfaced ? base + SURFACED_DETECTION_PENALTY : base;
    }

    /**
     * Advances the boat by one turn.
     *
     * <p>Submerged, it burns a point of oxygen and is forced up when it hits zero. Surfaced,
     * it takes air back on.
     *
     * @return true when this turn forced the boat to the surface, so the caller can say so
     */
    public boolean tickTurn() {
        if (surfaced) {
            oxygen = Math.min(maxOxygen, oxygen + OXYGEN_REGEN_PER_TURN);
            return false;
        }

        oxygen = Math.max(0, oxygen - 1);
        if (oxygen == 0) {
            surfaced = true;
            return true;
        }
        return false;
    }

    /** Brings the boat up by choice. Detection rises immediately. */
    public void surface() {
        surfaced = true;
    }

    /**
     * Takes the boat back down.
     *
     * @return false when there is not enough air left to dive yet
     */
    public boolean dive() {
        if (oxygen < OXYGEN_TO_DIVE) return false;
        surfaced = false;
        return true;
    }

    /** Restores the boat to a full tank, submerged. Used when a stage or a load begins. */
    public void resetOxygen() {
        this.oxygen = maxOxygen;
        this.surfaced = false;
    }

    /** Puts an exact oxygen state back, for loading a save. */
    public void restoreOxygen(int oxygen, boolean surfaced) {
        this.oxygen = Math.max(0, Math.min(maxOxygen, oxygen));
        this.surfaced = surfaced;
    }

    @Override
    public String toString() {
        return super.toString() + (surfaced ? " [surfaced]" : " [submerged O2 " + oxygen + "]");
    }
}
