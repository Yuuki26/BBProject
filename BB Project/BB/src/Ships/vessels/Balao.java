package Ships.vessels;

import Ships.ShipStats;
import Ships.Submarine;

/** Balao (SS) - a deep-diving patrol boat with the largest oxygen reserve of the two. */
public class Balao extends Submarine {
    public Balao() {
        super("Balao", new ShipStats()
                .hp(400).shields(1).dmg(500).shots(1)
                .penetration(90).detection(1).size(2).cost(5)
                .image("/ships/vessels/Balao.png"),
                4);
    }
}
