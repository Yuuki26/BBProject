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
                .hp(11).shields(2).dmg(6).shots(4)
                .penetration(4).detection(6).size(4));
    }
}
