package com.bb;

import java.awt.Point;

/**
 * What one shot found.
 *
 * <p>Before this existed the enemy fired into the void: {@code applyShots} resolved a salvo
 * and returned nothing, so no AI could ever be smarter than uniform random. Handing the
 * result back is what makes a searching opponent possible.
 */
public final class ShotOutcome {

    private final Point tile;
    private final boolean hit;
    private final boolean sunk;
    private final int concealment;

    /**
     * @param tile        the tile fired at, as (x=column, y=row)
     * @param hit         whether a ship was there
     * @param sunk        whether that shot finished the ship off
     * @param concealment the hull's stealth, or 0 on a miss. Only meaningful once the tile is
     *                    already revealed, so passing it back leaks nothing.
     */
    public ShotOutcome(Point tile, boolean hit, boolean sunk, int concealment) {
        this.tile = new Point(tile);
        this.hit = hit;
        this.sunk = sunk;
        this.concealment = concealment;
    }

    public Point getTile() {
        return new Point(tile);
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isSunk() {
        return sunk;
    }

    public int getConcealment() {
        return concealment;
    }

    @Override
    public String toString() {
        return (char) ('A' + tile.x) + "" + (tile.y + 1)
                + (hit ? (sunk ? " SUNK" : " HIT") : " miss");
    }
}
