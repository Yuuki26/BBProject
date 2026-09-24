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
 *         .penetration(4).detection(6).size(4));
 * </pre>
 *
 * <p>Cost is deliberately absent: it belongs to the hull class, not the individual ship, and
 * is fixed by the abstract class that declares it.
 */
public class ShipStats {

    private int hp = 1;
    private int shields = 0;
    private int dmg = 1;
    private int shots = 1;
    private int penetration = 1;
    private int detection = 3;
    private int size = 1;

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

    public int hp() { return hp; }
    public int shields() { return shields; }
    public int dmg() { return dmg; }
    public int shots() { return shots; }
    public int penetration() { return penetration; }
    public int detection() { return detection; }
    public int size() { return size; }
}
