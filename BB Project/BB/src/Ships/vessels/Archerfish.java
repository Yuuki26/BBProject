package Ships.vessels;

import Ships.ShipStats;
import Ships.Submarine;

/**
 * Archerfish (SS) - heavier torpedoes than Balao and better at getting through armour, with
 * a shorter dive to spend them on.
 */
public class Archerfish extends Submarine {
    public Archerfish() {
        super("Archerfish", new ShipStats()
                .hp(450).shields(1).dmg(700).shots(1)
                .penetration(89).detection(1).size(2).cost(5)
                .image("/ships/vessels/Archerfish.png"),
                5);
    }
}
