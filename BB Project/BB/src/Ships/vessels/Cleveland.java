package Ships.vessels;

import Ships.LightCruiser;
import Ships.ShipStats;

/** Cleveland (CL) - the sturdier light cruiser, built to soak rather than to sprint. */
public class Cleveland extends LightCruiser {
    public Cleveland() {
        super("Cleveland", new ShipStats()
                .hp(8).shields(4).dmg(4).shots(2)
                .penetration(3).detection(4).size(3));
    }
}
