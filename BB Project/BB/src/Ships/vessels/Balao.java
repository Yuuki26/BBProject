package Ships.vessels;

import Ships.ShipStats;
import Ships.Submarine;

/** Balao (SS) - a deep-diving patrol boat with the largest oxygen reserve of the two. */
public class Balao extends Submarine {
    public Balao() {
        super("Balao", new ShipStats()
                .hp(4).shields(1).dmg(5).shots(1)
                .penetration(6).detection(1).size(2),
                6);
    }
}
