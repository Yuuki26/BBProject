package Ships.vessels;

import Ships.Destroyer;
import Ships.ShipStats;

/** I-141 (DD) - a little tougher than I-556, with a heavier but slower gun. */
public class I_141 extends Destroyer {
    public I_141() {
        super("I-141", new ShipStats()
                .hp(450).shields(12).dmg(280).shots(2)
                .penetration(12).detection(2).size(2).cost(2)
                .image("/ships/vessels/I_141.png"));
    }
}
