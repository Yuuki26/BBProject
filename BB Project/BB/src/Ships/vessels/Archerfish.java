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
                .hp(4).shields(1).dmg(7).shots(1)
                .penetration(8).detection(1).size(2),
                5);
    }
}
