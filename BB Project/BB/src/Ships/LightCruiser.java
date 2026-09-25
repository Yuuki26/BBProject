package Ships;

/**
 * Light cruiser (CL): fast six-inch batteries that trade armour and penetration for volume
 * of fire.
 */
public abstract class LightCruiser extends AbstractShip {

    protected LightCruiser(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public String getHullClass() {
        return "Light Cruiser";
    }

    @Override
    public String getHullCode() {
        return "CL";
    }

    /** Hull artwork, shown for any vessel of this class that has no picture of its own yet. */
    @Override
    protected String defaultImage() {
        return "/ships/battleCrusier.png";
    }
}
