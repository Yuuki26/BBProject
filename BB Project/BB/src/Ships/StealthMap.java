package Ships;

/**
 * Per-tile detection values for one board.
 *
 * <p>Built from the fleet rather than from terrain: a tile's value is the
 * {@link Ships_Type#getDetection() detection} of the ship sitting on it, and
 * {@link #OPEN_WATER} for empty water. That is what sets each tile's colour on the player's
 * board and the weight a searching opponent pays there.
 *
 * <p>It is rebuilt whenever the fleet changes - a ship moved, a submarine surfacing - so it
 * always reflects the current state rather than a snapshot taken at deployment.
 *
 * <h2>Why the opponent cannot simply read it</h2>
 * Because values come from ships, a raw copy of this map <em>is</em> the fleet layout.
 * {@code com.bb.EliteEnemy} therefore never reads a tile it has not already fired at; it
 * treats everything unrevealed as {@link #OPEN_WATER}. Detection still shapes the hunt once
 * contact is made, which is where it matters, without handing over the answer.
 */
public class StealthMap {

    /** Detection value for a tile with no ship on it. */
    public static final int OPEN_WATER = 0;

    /** Highest detection the colour ramp distinguishes. */
    public static final int MAX_DETECTION = 8;

    private final int size;
    private final int[][] detection;

    /** An empty board of open water. */
    public StealthMap(int size) {
        this.size = size;
        this.detection = new int[size][size];
    }

    /** Wraps an existing grid, used when loading a save. */
    public StealthMap(int[][] grid) {
        this.size = grid.length;
        this.detection = new int[size][size];
        for (int r = 0; r < size; r++) {
            System.arraycopy(grid[r], 0, this.detection[r], 0, Math.min(size, grid[r].length));
        }
    }

    /**
     * Builds a map from a board of placements.
     *
     * @param board tile grid indexed {@code [row][col]}; null entries are open water
     */
    public static StealthMap fromBoard(Ship_Placement[][] board) {
        int size = board.length;
        StealthMap map = new StealthMap(size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Ship_Placement sp = board[r][c];
                map.detection[r][c] = (sp == null || sp.getShip() == null)
                        ? OPEN_WATER
                        : Math.max(0, sp.getShip().getDetection());
            }
        }
        return map;
    }

    public int getSize() {
        return size;
    }

    /** Detection at a tile, or {@link #OPEN_WATER} for anything off the board. */
    public int at(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size) return OPEN_WATER;
        return detection[row][col];
    }

    /**
     * What a search pays to sweep a tile.
     *
     * <p>Inverted from detection: a loud ship is cheap to find, a quiet one is expensive.
     * Always at least 1, so no tile is free and a weighted search cannot loop for nothing.
     */
    public int cost(int row, int col) {
        return Math.max(1, MAX_DETECTION - at(row, col));
    }

    /** A defensive copy of the raw grid, for saving. */
    public int[][] toGrid() {
        int[][] copy = new int[size][size];
        for (int r = 0; r < size; r++) {
            System.arraycopy(detection[r], 0, copy[r], 0, size);
        }
        return copy;
    }
}
