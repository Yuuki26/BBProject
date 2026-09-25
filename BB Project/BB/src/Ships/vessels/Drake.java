package Ships.vessels;

import Ships.LightCruiser;
import Ships.ShipStats;

public class Drake extends LightCruiser {
    public Drake() {
        super("Drake", new ShipStats()
                .hp(1100).shields(35).dmg(90).shots(3)
                .penetration(40).detection(5).size(3).cost(5)
                .image("/ships/vessels/Drake.png"));
    }
}
