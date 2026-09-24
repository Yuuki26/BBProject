package Ships.vessels;

import Ships.HeavyCruiser;
import Ships.ShipStats;

/** Baltimore (CB) - the well-rounded heavy cruiser. */
public class Baltimore extends HeavyCruiser {
    public Baltimore() {
        super("Baltimore", new ShipStats()
                .hp(10).shields(5).dmg(6).shots(2)
                .penetration(5).detection(5).size(3));
    }
}
