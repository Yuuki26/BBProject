package com.bb;

import Ships.PlayerFleet;
import Ships.Ship_Placement;
import Ships.Ships_Type;
import Ships.vessels.VesselRegistry;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds the enemy fleet for a stage.
 *
 * <p>Fleets are budgeted in <b>cost</b>, not tile count. Cost is the fixed per-vessel
 * number on {@link Ships_Type#getCost()}, so a budget of 20 buys one battleship and a
 * destroyer, or five light cruisers, and both are a fair match for the same player fleet.
 * Sizing by tiles would have made a board of cheap destroyers look identical to a board of
 * capital ships.
 *
 * <p>The budget is the stage's base deployment budget - without any fleet expansions the
 * player has bought - and heavier hull classes unlock as the run goes on.
 */
public class OpponentGenerator {

    private final int boardSize;
    private final Random rand = new Random();

    /** Stage at which each hull class starts appearing in enemy fleets. */
    private static final int DESTROYER_FROM = 1;
    private static final int LIGHT_CRUISER_FROM = 1;
    private static final int SUBMARINE_FROM = 2;
    private static final int HEAVY_CRUISER_FROM = 3;
    private static final int CARRIER_FROM = 5;
    private static final int BATTLESHIP_FROM = 6;

    public OpponentGenerator(int boardSize) {
        this.boardSize = boardSize;
    }

    /** Builds the fleet for the stage the run is currently on. */
    public List<Ship_Placement> buildOpponentFleet(PlayerFleet playerFleet) {
        return buildOpponentFleet(playerFleet, RunState.current().getStage());
    }

    /**
     * Builds an enemy fleet for an explicit stage.
     *
     * <p>The enemy is bought out of the <em>same</em> cost budget the player deploys under.
     * That is what makes cost the balancing lever: both sides field the same tonnage of hulls
     * each stage, and the only difference is which ones and where.
     *
     * @param playerFleet the player's roster; unused for sizing, kept for future tuning
     * @param stage       1-based stage number; higher means a bigger budget and heavier hulls
     */
    public List<Ship_Placement> buildOpponentFleet(PlayerFleet playerFleet, int stage) {
        int budget = Math.max(VesselRegistry.cheapestCost(),
                RunState.deploymentBudgetForStage(stage));

        List<Ships_Type> composition = buyFleet(budget, stage);
        return placeOnBoard(composition);
    }

    /** Hull codes available at {@code stage}. */
    private List<String> unlockedHulls(int stage) {
        List<String> hulls = new ArrayList<>();
        if (stage >= DESTROYER_FROM) hulls.add("DD");
        if (stage >= LIGHT_CRUISER_FROM) hulls.add("CL");
        if (stage >= SUBMARINE_FROM) hulls.add("SS");
        if (stage >= HEAVY_CRUISER_FROM) hulls.add("CB");
        if (stage >= CARRIER_FROM) hulls.add("CV");
        if (stage >= BATTLESHIP_FROM) hulls.add("BB");
        return hulls;
    }

    /** Every vessel of an unlocked hull class, as catalogue entries. */
    private List<VesselRegistry.Entry> availableVessels(int stage) {
        List<VesselRegistry.Entry> out = new ArrayList<>();
        for (String hull : unlockedHulls(stage)) {
            out.addAll(VesselRegistry.byHull(hull));
        }
        return out;
    }

    /**
     * Spends the budget on hulls.
     *
     * <p>Buys randomly from what is unlocked and affordable, then spends whatever is left on
     * the cheapest hull that still fits, so the budget is used up rather than abandoned part
     * way. The board also caps how much can physically be placed.
     */
    private List<Ships_Type> buyFleet(int budget, int stage) {
        List<VesselRegistry.Entry> catalogue = availableVessels(stage);
        List<Ships_Type> picked = new ArrayList<>();
        if (catalogue.isEmpty()) return picked;

        // Never fill more than 40% of the board, or placement starts failing.
        int tileCap = (int) (boardSize * boardSize * 0.4f);
        int tilesUsed = 0;
        int spent = 0;

        int guard = 200;
        while (spent < budget && guard-- > 0) {
            List<VesselRegistry.Entry> affordable = new ArrayList<>();
            for (VesselRegistry.Entry e : catalogue) {
                if (e.getCost() <= budget - spent) affordable.add(e);
            }
            if (affordable.isEmpty()) break;

            VesselRegistry.Entry entry = affordable.get(rand.nextInt(affordable.size()));
            Ships_Type ship = entry.create();
            if (tilesUsed + ship.getSize() > tileCap) {
                // No room for this hull; try to squeeze in something smaller instead.
                VesselRegistry.Entry smallest = smallestFitting(affordable, tileCap - tilesUsed);
                if (smallest == null) break;
                ship = smallest.create();
            }

            picked.add(ship);
            spent += ship.getCost();
            tilesUsed += ship.getSize();
        }

        // A stage with no enemy at all would be an instant win, so guarantee one hull.
        if (picked.isEmpty()) {
            picked.add(catalogue.get(rand.nextInt(catalogue.size())).create());
        }
        return picked;
    }

    private VesselRegistry.Entry smallestFitting(List<VesselRegistry.Entry> options, int tilesLeft) {
        VesselRegistry.Entry best = null;
        int bestSize = Integer.MAX_VALUE;
        for (VesselRegistry.Entry e : options) {
            int size = e.create().getSize();
            if (size <= tilesLeft && size < bestSize) {
                best = e;
                bestSize = size;
            }
        }
        return best;
    }

    private List<Ship_Placement> placeOnBoard(List<Ships_Type> ships) {
        boolean[][] occupied = new boolean[boardSize][boardSize];
        List<Ship_Placement> placements = new ArrayList<>();

        // Largest first: big hulls are hardest to fit once the board fills up.
        ships.sort((a, b) -> Integer.compare(b.getSize(), a.getSize()));

        for (Ships_Type st : ships) {
            Ship_Placement sp = new Ship_Placement(st, null, rand.nextBoolean());
            if (tryPlace(sp, occupied, 500)) {
                placements.add(sp);
                continue;
            }
            // Flip it and try again before giving up on this hull.
            sp.rotate();
            if (tryPlace(sp, occupied, 200)) {
                placements.add(sp);
            }
        }
        return placements;
    }

    private boolean tryPlace(Ship_Placement sp, boolean[][] occupied, int tries) {
        int size = sp.getShip().getSize();
        while (tries-- > 0) {
            int maxX = sp.isHorizontal() ? boardSize - size : boardSize - 1;
            int maxY = sp.isHorizontal() ? boardSize - 1 : boardSize - size;
            if (maxX < 0 || maxY < 0) return false;

            sp.setOrigin(new Point(rand.nextInt(maxX + 1), rand.nextInt(maxY + 1)));
            if (fits(sp, occupied)) {
                for (Point p : sp.getOccupiedTiles()) {
                    occupied[p.y][p.x] = true;
                }
                return true;
            }
        }
        sp.setOrigin(null);
        return false;
    }

    private boolean fits(Ship_Placement sp, boolean[][] occ) {
        for (Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= boardSize || p.y < 0 || p.y >= boardSize) return false;
            if (occ[p.y][p.x]) return false;
        }
        return true;
    }
}
