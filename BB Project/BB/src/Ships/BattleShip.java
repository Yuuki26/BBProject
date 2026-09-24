package Ships;

/**
 * Battleship (BB): the heaviest guns and the thickest belt armour in the game, and the most
 * expensive hull to field.
 */
public abstract class Battleship extends AbstractShip {

    /** Fixed build cost for every battleship. */
    public static final int COST = 10;

    protected Battleship(String name, ShipStats stats) {
        super(name, stats);
    }

    @Override
    public final int getCost() {
        return COST;
    }

    @Override
    public String getHullClass() {
        return "Battleship";
    }

    @Override
    public String getHullCode() {
        return "BB";
    }

    @Override
    public String getImage() {
        return "/ships/BattleShip_Class.png";
    }
}
