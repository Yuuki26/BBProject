package Ships;

/**
 * The numbers that make up one vessel, set once at construction.
 *
 * <p>A named ship reads as a short block of labelled values rather than a positional
 * constructor call with eight anonymous integers:
 *
 * <pre>
 * super("Enterprise", new ShipStats()
 *         .hp(11).shields(2).dmg(6).shots(4)
 *         .penetration(4).detection(6).size(4).cost(8));
 * </pre>
 *
 * <p>Every vessel sets its own {@link #cost(int) cost}, so two ships of the same class can be
 * priced differently. It has no usable default: a vessel that leaves it out is refused when
 * it is built (see {@link AbstractShip}), rather than quietly sailing for free.
 */
public class ShipStats {

    private int hp = 1;
    private int shields = 0;
    private int dmg = 1;
    private int shots = 1;
    private int penetration = 1;
    private int detection = 3;
    private int size = 1;
    private int cost = 0;
    private String image;

    public ShipStats hp(int value) {
        this.hp = value;
        return this;
    }

    public ShipStats shields(int value) {
        this.shields = value;
        return this;
    }

    public ShipStats dmg(int value) {
        this.dmg = value;
        return this;
    }

    public ShipStats shots(int value) {
        this.shots = value;
        return this;
    }

    public ShipStats penetration(int value) {
        this.penetration = value;
        return this;
    }

    /** How easily the enemy finds this ship. Higher is more visible. */
    public ShipStats detection(int value) {
        this.detection = value;
        return this;
    }

    public ShipStats size(int value) {
        this.size = value;
        return this;
    }

    /** What this vessel costs to deploy, out of the stage's budget. Must be at least 1. */
    public ShipStats cost(int value) {
        this.cost = value;
        return this;
    }

    /**
     * This vessel's own picture, as a path under {@code lib}, e.g.
     * {@code "/ships/vessels/Nagato.png"}. Until that file exists the hull's picture is shown.
     */
    public ShipStats image(String path) {
        this.image = path;
        return this;
    }

    public int hp() { return hp; }
    public int shields() { return shields; }
    public int dmg() { return dmg; }
    public int shots() { return shots; }
    public int penetration() { return penetration; }
    public int detection() { return detection; }
    public int size() { return size; }
    public int cost() { return cost; }
    public String image() { return image; }
}
