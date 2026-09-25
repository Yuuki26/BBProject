package Ships.vessels;

import Ships.LightCruiser;
import Ships.ShipStats;

/** Cleveland (CL) - the sturdier light cruiser, built to soak rather than to sprint. */
public class Cleveland extends LightCruiser {
    public Cleveland() {
        super("Cleveland", new ShipStats()
                .hp(850).shields(40).dmg(55).shots(3)
                .penetration(35).detection(4).size(3).cost(4)
                .image("/ships/vessels/Cleveland.png"));
    }
}
