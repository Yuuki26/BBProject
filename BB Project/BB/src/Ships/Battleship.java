package Ships;

/**
 * Battleship (BB): the heaviest guns and the thickest belt armour in the game, and the most
 * expensive hull to field.
 */
public abstract class Battleship extends AbstractShip {

    protected Battleship(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public String getHullClass() {
        return "Battleship";
    }

    @Override
    public String getHullCode() {
        return "BB";
    }

    /** Hull artwork, shown for any vessel of this class that has no picture of its own yet. */
    @Override
    protected String defaultImage() {
        return "/ships/BattleShip_Class.png";
    }
}
