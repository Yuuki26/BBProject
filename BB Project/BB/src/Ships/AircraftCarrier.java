package Ships;

/**
 * Aircraft carrier (CV): the longest reach in the fleet and the heaviest strike, paid for
 * with a large silhouette and thin armour.
 */
public abstract class AircraftCarrier extends AbstractShip {

    /** Fixed build cost for every carrier. */
    public static final int COST = 8;

    protected AircraftCarrier(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public final int getCost() {
        return COST;
    }

    @Override
    public String getHullClass() {
        return "Aircraft Carrier";
    }

    @Override
    public String getHullCode() {
        return "CV";
    }

    @Override
    public String getImage() {
        return "/ships/motherShip.png";
    }
}
