package com.bb;

import Ships.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OpponentGenerator {
    private final int boardSize;
    private final Random rand = new Random();

    // Available opponent ship types to pick from (add yours here)
    private final List<Class<? extends Ships_Type>> availableTypes = new ArrayList<>();
    public OpponentGenerator(int boardSize) {
        this.boardSize = boardSize;

        // Register opponent ship types here
        availableTypes.add(BattleCruiser.class);
        availableTypes.add(Submarine.class);

    }

    public List<Ship_Placement> buildOpponentFleet(DefaultFleet playerFleet) {
        int playerTotal = totalSizeOf(playerFleet);
        int minTarget = Math.max(1, playerTotal / 2);       // at least half
        int maxTarget = Math.max(minTarget, playerTotal);    // at most player's total

        // Randomize composition to meet target in [minTarget, maxTarget]
        List<Ships_Type> composition = randomComposition(minTarget, maxTarget);

        // Create placements with random orientation and place onto board (no collisions)
        return placeOnBoard(composition);
    }

    private int totalSizeOf(DefaultFleet df) {
        int sum = 0;
        for (Ship_Placement sp : df.getPlacements()) {
            sum += sp.getShip().getSize();
        }
        return sum;
    }

    private List<Ships_Type> randomComposition(int minTarget, int maxTarget) {
        List<Ships_Type> picked = new ArrayList<>();
        int total = 0;

        // We’ll aim for a random target between min and max, then fill up to that target
        int target = minTarget + rand.nextInt(Math.max(1, (maxTarget - minTarget + 1)));

        // Greedy/random fill without exceeding target
        int attempts = 1000; // safety bound
        while (total < target && attempts-- > 0) {
            Class<? extends Ships_Type> typeClass = availableTypes.get(rand.nextInt(availableTypes.size()));
            Ships_Type ship = newInstance(typeClass);
            if (ship == null) continue;

            int size = ship.getSize();
            if (total + size <= target) {
                picked.add(ship);
                total += size;
            } else {
                // try a smaller ship type if any exist
                boolean addedSmaller = false;
                for (Class<? extends Ships_Type> cls : availableTypes) {
                    Ships_Type s2 = newInstance(cls);
                    if (s2 != null && total + s2.getSize() <= target) {
                        picked.add(s2);
                        total += s2.getSize();
                        addedSmaller = true;
                        break;
                    }
                }
                if (!addedSmaller) break; // can’t fit anything else
            }
        }

        // Ensure we never drop below minTarget (edge case)
        if (total < minTarget) {
            // try to bump with smallest fitting ships
            for (Class<? extends Ships_Type> cls : availableTypes) {
                Ships_Type s2 = newInstance(cls);
                if (s2 != null && total + s2.getSize() <= maxTarget) {
                    picked.add(s2);
                    total += s2.getSize();
                    if (total >= minTarget) break;
                }
            }
        }

        return picked;
    }

    private Ships_Type newInstance(Class<? extends Ships_Type> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Ship_Placement> placeOnBoard(List<Ships_Type> ships) {
        boolean[][] occupied = new boolean[boardSize][boardSize];
        List<Ship_Placement> placements = new ArrayList<>();

        for (Ships_Type st : ships) {
            Ship_Placement sp = new Ship_Placement(st, null, rand.nextBoolean());

            boolean placed = false;
            int tries = 500;
            while (!placed && tries-- > 0) {
                // choose origin respecting bounds
                int size = st.getSize();
                boolean horizontal = sp.isHorizontal();
                int maxX = horizontal ? boardSize - size : boardSize - 1;
                int maxY = horizontal ? boardSize - 1 : boardSize - size;
                int x = rand.nextInt(maxX + 1);
                int y = rand.nextInt(maxY + 1);
                sp.setOrigin(new Point(x, y));

                if (fits(sp, occupied)) {
                    // mark occupied cells and accept
                    for (Point p : sp.getOccupiedTiles()) {
                        occupied[p.y][p.x] = true;
                    }
                    placements.add(sp);
                    placed = true;
                }
            }
            // If not placed after many tries, you can skip or retry with different orientation
            if (!placed) {
                // optional: flip orientation once more and retry briefly
                sp.rotate();
                tries = 200;
                while (!placed && tries-- > 0) {
                    int size = st.getSize();
                    boolean horizontal = sp.isHorizontal();
                    int maxX = horizontal ? boardSize - size : boardSize - 1;
                    int maxY = horizontal ? boardSize - 1 : boardSize - size;
                    int x = rand.nextInt(maxX + 1);
                    int y = rand.nextInt(maxY + 1);
                    sp.setOrigin(new Point(x, y));
                    if (fits(sp, occupied)) {
                        for (Point p : sp.getOccupiedTiles()) {
                            occupied[p.y][p.x] = true;
                        }
                        placements.add(sp);
                        placed = true;
                    }
                }
            }
        }
        return placements;
    }

    private boolean fits(Ship_Placement sp, boolean[][] occ) {
        for (Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= boardSize || p.y < 0 || p.y >= boardSize) return false;
            if (occ[p.y][p.x]) return false;
        }
        return true;
    }
}
