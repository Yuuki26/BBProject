package Ships.vessels;

import Ships.HeavyCruiser;
import Ships.ShipStats;

/** Baltimore (CB) - the well-rounded heavy cruiser. */
public class Baltimore extends HeavyCruiser {
    public Baltimore() {
        super("Baltimore", new ShipStats()
                .hp(1050).shields(55).dmg(125).shots(3)
                .penetration(60).detection(5).size(3).cost(5)
                .image("/ships/vessels/Baltimore.png"));
    }
}
