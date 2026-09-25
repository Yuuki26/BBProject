package com.bb;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The baseline opponent: fires at uniformly random tiles it has not tried yet.
 *
 * <p>It ignores {@link #reportResults}, so it never follows up on a hit. {@link EliteEnemy}
 * is the one that searches.
 */
public class NormalEnenmy implements EnemyAI {

    private final int board;
    private final Random random = new Random();
    private final boolean[][] hasfired;

    public NormalEnenmy(int board) {
        this.board = board;
        this.hasfired = new boolean[board][board];
    }

    @Override
    public String getName() {
        return "Standard";
    }

    @Override
    public List<Point> generateShots(int maxShots) {
        return generateShots(maxShots, null);
    }

    /** Random, as ever - re-fire targets simply join the pool of tiles it picks from. */
    @Override
    public List<Point> generateShots(int maxShots, List<Point> refireTargets) {
        List<Point> available = new ArrayList<>();
        for (int r = 0; r < board; r++) {
            for (int c = 0; c < board; c++) {
                if (!hasfired[r][c]) {
                    available.add(new Point(c, r));
                }
            }
        }
        if (refireTargets != null) {
            for (Point p : refireTargets) {
                if (!available.contains(p)) available.add(new Point(p));
            }
        }
        Collections.shuffle(available, random);

        int n = Math.min(maxShots, available.size());
        List<Point> chosen = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Point p = available.get(i);
            hasfired[p.y][p.x] = true;
            chosen.add(p);
        }
        return chosen;
    }
}
