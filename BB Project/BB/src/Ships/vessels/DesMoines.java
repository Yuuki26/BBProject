package Ships.vessels;

import Ships.HeavyCruiser;
import Ships.ShipStats;

/**
 * Des Moines (CB) - autoloading eight-inch guns, so she fires a third salvo where other
 * heavy cruisers manage two.
 */
public class DesMoines extends HeavyCruiser {
    public DesMoines() {
        super("Des Moines", new ShipStats()
                .hp(11).shields(5).dmg(7).shots(3)
                .penetration(5).detection(5).size(3));
    }
}
