package Ships;

/**
 * Destroyer (DD): the cheapest hull on the board. Fragile, but small enough to be awkward to
 * find and cheap enough to field in numbers.
 */
public abstract class Destroyer extends AbstractShip {

    /** Fixed build cost for every destroyer. */
    public static final int COST = 2;

    protected Destroyer(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public final int getCost() {
        return COST;
    }

    @Override
    public String getHullClass() {
        return "Destroyer";
    }

    @Override
    public String getHullCode() {
        return "DD";
    }

    @Override
    public String getImage() {
        return "/ships/submarine.png";
    }
}
