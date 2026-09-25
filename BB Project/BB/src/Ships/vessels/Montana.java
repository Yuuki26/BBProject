package Ships.vessels;

import Ships.Battleship;
import Ships.ShipStats;

/** Montana (BB) - the toughest hull afloat, trading a little punch for survivability. */
public class Montana extends Battleship {
    public Montana() {
        super("Montana", new ShipStats()
                .hp(1600).shields(95).dmg(365).shots(2)
                .penetration(120).detection(7).size(4).cost(12)
                .image("/ships/vessels/Montana.png"));
    }
}
