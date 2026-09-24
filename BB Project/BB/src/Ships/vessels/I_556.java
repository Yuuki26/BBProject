package Ships.vessels;

import Ships.Destroyer;
import Ships.ShipStats;

/** I-556 (DD) - a screening destroyer: three light salvos rather than one heavy one. */
public class I_556 extends Destroyer {
    public I_556() {
        super("I-556", new ShipStats()
                .hp(4).shields(1).dmg(2).shots(3)
                .penetration(2).detection(4).size(2));
    }
}
