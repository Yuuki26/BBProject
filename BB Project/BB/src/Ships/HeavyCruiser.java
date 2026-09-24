package Ships;

/**
 * Heavy cruiser (CB): eight-inch guns and real armour, without a capital ship's price tag.
 */
public abstract class HeavyCruiser extends AbstractShip {

    /** Fixed build cost for every heavy cruiser. */
    public static final int COST = 6;

    protected HeavyCruiser(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public final int getCost() {
        return COST;
    }

    @Override
    public String getHullClass() {
        return "Heavy Cruiser";
    }

    @Override
    public String getHullCode() {
        return "CB";
    }

    @Override
    public String getImage() {
        return "/ships/battleCrusier.png";
    }
}
