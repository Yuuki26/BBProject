package Ships.vessels;

import Ships.AircraftCarrier;
import Ships.ShipStats;

/** Essex (CV) - the standard fleet carrier: a balanced air group and a long reach. */
public class Essex extends AircraftCarrier {
    public Essex() {
        super("Essex", new ShipStats()
                .hp(1600).shields(20).dmg(350).shots(3)
                .penetration(50).detection(6).size(4).cost(14)
                .image("/ships/vessels/Essex.png"));
    }
}
