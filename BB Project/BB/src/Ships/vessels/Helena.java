package Ships.vessels;

import Ships.LightCruiser;
import Ships.ShipStats;

/** Helena (CL) - radar-directed gunnery: more shots, less hull to absorb the reply. */
public class Helena extends LightCruiser {
    public Helena() {
        super("Helena", new ShipStats()
                .hp(6).shields(3).dmg(5).shots(3)
                .penetration(3).detection(4).size(3));
    }
}
