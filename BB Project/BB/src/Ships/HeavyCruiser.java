package Ships;

/**
 * Heavy cruiser (CB): eight-inch guns and real armour, without a capital ship's price tag.
 */
public abstract class HeavyCruiser extends AbstractShip {

    protected HeavyCruiser(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public String getHullClass() {
        return "Heavy Cruiser";
    }

    @Override
    public String getHullCode() {
        return "CB";
    }

    /** Hull artwork, shown for any vessel of this class that has no picture of its own yet. */
    @Override
    protected String defaultImage() {
        return "/ships/battleCrusier.png";
    }
}
