package Ships.vessels;

import Ships.Battleship;
import Ships.ShipStats;

/** Montana (BB) - the toughest hull afloat, trading a little punch for survivability. */
public class Montana extends Battleship {
    public Montana() {
        super("Montana", new ShipStats()
                .hp(16).shields(10).dmg(9).shots(2)
                .penetration(8).detection(7).size(4));
    }
}
