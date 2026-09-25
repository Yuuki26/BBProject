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
                .hp(1100).shields(50).dmg(90).shots(3)
                .penetration(55).detection(5).size(3).cost(5)
                .image("/ships/vessels/DesMoines.png"));
    }
}
