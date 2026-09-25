package Ships.vessels;

import Ships.Battleship;
import Ships.ShipStats;

//Mid-tier BB, strong Battleship, cost less, have less HP and moderate DMG, perfect for start/mid-game.
public class Monarch extends Battleship {
    public Monarch() {
        super("Monarch", new ShipStats()
                .hp(1000).shields(65).dmg(210).shots(2)
                .penetration(75).detection(6).size(4).cost(7)
                .image("/ships/vessels/Monarch.png"));
    }
}
