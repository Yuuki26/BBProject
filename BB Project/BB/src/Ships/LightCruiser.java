package Ships;

/**
 * Light cruiser (CL): fast six-inch batteries that trade armour and penetration for volume
 * of fire.
 */
public abstract class LightCruiser extends AbstractShip {

    /** Fixed build cost for every light cruiser. */
    public static final int COST = 4;

    protected LightCruiser(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public final int getCost() {
        return COST;
    }

    @Override
    public String getHullClass() {
        return "Light Cruiser";
    }

    @Override
    public String getHullCode() {
        return "CL";
    }

    @Override
    public String getImage() {
        return "/ships/battleCrusier.png";
    }
}
