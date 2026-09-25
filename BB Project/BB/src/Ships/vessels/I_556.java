package Ships.vessels;

import Ships.Destroyer;
import Ships.ShipStats;

/** I-556 (DD) - a screening destroyer: three light salvos rather than one heavy one. */
public class I_556 extends Destroyer {
    public I_556() {
        super("I-556", new ShipStats()
                .hp(350).shields(10).dmg(260).shots(3)
                .penetration(25).detection(2).size(2).cost(2)
                .image("/ships/vessels/I_556.png"));
    }
}
