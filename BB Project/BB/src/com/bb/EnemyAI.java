package com.bb;

import java.awt.Point;
import java.util.List;

/**
 * An opponent that picks targets on the player's board.
 *
 * <p>Implementations range from {@link NormalEnenmy}, which fires at random, to
 * {@link EliteEnemy}, which runs a weighted search over the board's stealth map.
 */
public interface EnemyAI {

    /** Shown in the status bar so the player can tell which opponent they are facing. */
    String getName();

    /**
     * Chooses up to {@code maxShots} distinct tiles that have not been fired at yet.
     *
     * @return tiles as (x=column, y=row); may be shorter than {@code maxShots} near the end
     *         of a battle, and empty when the board is exhausted
     */
    List<Point> generateShots(int maxShots);

    /**
     * Chooses up to {@code maxShots} distinct tiles: fresh ones, or tiles in
     * {@code refireTargets} - hits on a crippled or fully-hit ship, which the board lets the
     * opponent fire on again at half damage, the same rule the player plays by.
     *
     * <p>Defaults to ignoring the re-fires, for an opponent that never takes them.
     */
    default List<Point> generateShots(int maxShots, List<Point> refireTargets) {
        return generateShots(maxShots);
    }

    /**
     * Reports what the last salvo found.
     *
     * <p>Defaulted to a no-op so a memoryless opponent does not have to care.
     */
    default void reportResults(List<ShotOutcome> outcomes) {
        // ignored by default
    }
}
