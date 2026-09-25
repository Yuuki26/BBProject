package Ships;

/**
 * Aircraft carrier (CV): the longest reach in the fleet and the heaviest strike, paid for
 * with a large silhouette and thin armour.
 */
public abstract class AircraftCarrier extends AbstractShip {

    protected AircraftCarrier(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public String getHullClass() {
        return "Aircraft Carrier";
    }

    @Override
    public String getHullCode() {
        return "CV";
    }

    /** Hull artwork, shown for any vessel of this class that has no picture of its own yet. */
    @Override
    protected String defaultImage() {
        return "/ships/motherShip.png";
    }
}
