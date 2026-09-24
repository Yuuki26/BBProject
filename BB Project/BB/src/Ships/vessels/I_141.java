package Ships.vessels;

import Ships.Destroyer;
import Ships.ShipStats;

/** I-141 (DD) - a little tougher than I-556, with a heavier but slower gun. */
public class I_141 extends Destroyer {
    public I_141() {
        super("I-141", new ShipStats()
                .hp(5).shields(2).dmg(3).shots(2)
                .penetration(2).detection(4).size(2));
    }
}
