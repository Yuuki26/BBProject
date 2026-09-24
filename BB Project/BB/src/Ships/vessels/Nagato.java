package Ships.vessels;

import Ships.Battleship;
import Ships.ShipStats;

/** Nagato (BB) - hits harder and deeper than she is armoured for. */
public class Nagato extends Battleship {
    public Nagato() {
        super("Nagato", new ShipStats()
                .hp(13).shields(7).dmg(10).shots(2)
                .penetration(9).detection(7).size(4));
    }
}
