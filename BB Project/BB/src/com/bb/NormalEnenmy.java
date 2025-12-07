package com.bb;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NormalEnenmy {
    private final int board;
    private final Random random=new Random();

    private final boolean[][]hasfired;

    public NormalEnenmy(int board){
        this.board=board;
        this.hasfired=new boolean[board][board];
    }
    public List<Point> generateShots(int maxShots) {
        List<Point> available = new ArrayList<>();
        for (int r = 0; r < board; r++) {
            for (int c = 0; c < board; c++) {
                if (!hasfired[r][c]) {
                    available.add(new Point(c, r));
                }
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
