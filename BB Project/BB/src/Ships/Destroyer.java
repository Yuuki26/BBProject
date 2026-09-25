package Ships;

/**
 * Destroyer (DD): the cheapest hull on the board. Fragile, but small enough to be awkward to
 * find and cheap enough to field in numbers.
 */
public abstract class Destroyer extends AbstractShip {

    protected Destroyer(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public String getHullClass() {
        return "Destroyer";
    }

    @Override
    public String getHullCode() {
        return "DD";
    }

    /** Hull artwork, shown for any vessel of this class that has no picture of its own yet. */
    @Override
    protected String defaultImage() {
        return "/ships/submarine.png";
    }
}
