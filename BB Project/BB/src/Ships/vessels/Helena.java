package Ships.vessels;

import Ships.LightCruiser;
import Ships.ShipStats;

/** Helena (CL) - radar-directed gunnery: more shots, less hull to absorb the reply. */
public class Helena extends LightCruiser {
    public Helena() {
        super("Helena", new ShipStats()
                .hp(600).shields(30).dmg(100).shots(3)
                .penetration(30).detection(4).size(3).cost(4)
                .image("/ships/vessels/Helena.png"));
    }
}
