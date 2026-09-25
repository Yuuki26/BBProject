package Ships.vessels;

import Ships.Battleship;
import Ships.ShipStats;

/** Nagato (BB) - hits harder and deeper than she is armoured for. */
public class Nagato extends Battleship {
    public Nagato() {
        super("Nagato", new ShipStats()
                .hp(1300).shields(90).dmg(210).shots(2)
                .penetration(90).detection(7).size(4).cost(10)
                .image("/ships/vessels/Nagato.png"));
    }
}
