package Ships.vessels;

import Ships.AircraftCarrier;
import Ships.ShipStats;

/**
 * Enterprise (CV) - tougher than her sisters and able to put up a fourth strike, at the cost
 * of a little weight per attack.
 */
public class Enterprise extends AircraftCarrier {
    public Enterprise() {
        super("Enterprise", new ShipStats()
                .hp(1800).shields(35).dmg(600).shots(2)
                .penetration(55).detection(6).size(4).cost(15)
                .image("/ships/vessels/Enterprise.png"));
    }
}
