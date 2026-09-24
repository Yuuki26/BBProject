package Ships;

import Ships.vessels.Baltimore;
import Ships.vessels.Balao;
import Ships.vessels.Enterprise;
import Ships.vessels.Helena;
import Ships.vessels.I_556;
import Ships.vessels.Nagato;

import java.util.ArrayList;
import java.util.List;

/**
 * The player's roster of ships.
 *
 * <p>Placements start with a {@code null} origin, which is what marks a ship as still
 * undeployed - it is in the roster but not yet on the board.
 */
public class DefaultFleet {

    /**
     * Cost of the starting fleet. Enemy fleets are budgeted against this, so changing the
     * line-up below automatically rebalances the whole run.
     */
    private final List<Ship_Placement> fleet = new ArrayList<>();

    public DefaultFleet() {
        reset();
    }

    /**
     * Restores the starting roster, undeployed.
     *
     * <p>One hull of every class, 33 cost in total. That is deliberately far more than the
     * stage 1 deployment budget of 10 can field: the roster is what the player owns, and the
     * budget decides what actually sails. Ten buys the battleship alone, or the carrier plus
     * the destroyer, or the two cruisers - which is the choice the whole system exists for.
     */
    public final void reset() {
        fleet.clear();
        fleet.add(new Ship_Placement(new Nagato(), null, true));     // BB, cost 10
        fleet.add(new Ship_Placement(new Enterprise(), null, true)); // CV, cost 8
        fleet.add(new Ship_Placement(new Baltimore(), null, true));  // CB, cost 6
        fleet.add(new Ship_Placement(new Helena(), null, true));     // CL, cost 4
        fleet.add(new Ship_Placement(new Balao(), null, true));      // SS, cost 3
        fleet.add(new Ship_Placement(new I_556(), null, true));      // DD, cost 2
    }

    public List<Ship_Placement> getPlacements() {
        return fleet;
    }

    /** Adds a ship to the roster, undeployed. Used when a run rewards a new hull. */
    public void addShip(Ships_Type type) {
        if (type == null) return;
        fleet.add(new Ship_Placement(type, null, true));
    }

    /** Replaces the roster wholesale. Used when loading a save. */
    public void setPlacements(List<Ship_Placement> placements) {
        fleet.clear();
        if (placements != null) fleet.addAll(placements);
    }

    /** Combined tile footprint of the whole roster. */
    public int totalSize() {
        int sum = 0;
        for (Ship_Placement sp : fleet) {
            if (sp.getShip() != null) sum += sp.getShip().getSize();
        }
        return sum;
    }

    /**
     * Combined build cost of the whole roster.
     *
     * <p>This, not tile count, is what enemy fleets are sized against: it is the measure that
     * makes six destroyers and two battleships comparable.
     */
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
