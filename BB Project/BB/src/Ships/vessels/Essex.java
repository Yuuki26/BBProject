package Ships.vessels;

import Ships.AircraftCarrier;
import Ships.ShipStats;

/** Essex (CV) - the standard fleet carrier: a balanced air group and a long reach. */
public class Essex extends AircraftCarrier {
    public Essex() {
        super("Essex", new ShipStats()
                .hp(9).shields(2).dmg(7).shots(3)
                .penetration(4).detection(6).size(4));
    }
}
