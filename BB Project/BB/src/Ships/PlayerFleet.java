package Ships;

import java.util.ArrayList;
import java.util.List;

/**
 * The ships the player owns: the roster.
 *
 * <p>A run starts with this empty. The player fills it by choosing a starter fleet
 * ({@code com.bb.StarterFleetPanel}), and after that the only ways it changes are buying and
 * selling in the shop. There is no default line-up any more.
 *
 * <p>Placements start with a {@code null} origin, which is what marks a ship as still
 * undeployed - it is in the roster but not yet on the board.
 */
public class PlayerFleet {

    private final List<Ship_Placement> fleet = new ArrayList<>();

    /** Empties the roster, ready for a new run's starter pick. */
    public final void reset() {
        fleet.clear();
    }

    public List<Ship_Placement> getPlacements() {
        return fleet;
    }

    /** Adds a ship to the roster, undeployed. */
    public Ship_Placement addShip(Ships_Type type) {
        if (type == null) return null;
        Ship_Placement sp = new Ship_Placement(type, null, true);
        fleet.add(sp);
        return sp;
    }

    /** Takes a ship out of the roster entirely, as when it is sold. */
    public boolean removeShip(Ship_Placement sp) {
        return fleet.remove(sp);
    }

    /** Replaces the roster wholesale. Used for the starter pick and when loading a save. */
    public void setPlacements(List<Ship_Placement> placements) {
        fleet.clear();
        if (placements != null) fleet.addAll(placements);
    }

    public int size() {
        return fleet.size();
    }

    /** Combined tile footprint of the whole roster. */
    public int totalSize() {
        int sum = 0;
        for (Ship_Placement sp : fleet) {
            if (sp.getShip() != null) sum += sp.getShip().getSize();
        }
        return sum;
    }

    /** Combined deployment cost of the whole roster. */
    public int totalCost() {
        int sum = 0;
        for (Ship_Placement sp : fleet) {
            if (sp.getShip() != null) sum += sp.getShip().getCost();
        }
        return sum;
    }

    /** Every submarine in the roster, for the per-turn oxygen tick. */
    public List<Submarine> submarines() {
        List<Submarine> out = new ArrayList<>();
        for (Ship_Placement sp : fleet) {
            if (sp.getShip() instanceof Submarine) out.add((Submarine) sp.getShip());
        }
        return out;
    }
}
