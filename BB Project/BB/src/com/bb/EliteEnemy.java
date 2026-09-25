package com.bb;

import Ships.StealthMap;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * An opponent that actually searches, instead of firing at random.
 *
 * <p>It keeps two maps, which together are all the state the search needs:
 *
 * <ul>
 *   <li>{@code detectionWeight} - what each tile costs to sweep, derived from the
 *       {@link Ships.Ships_Type#getDetection() detection} of whatever was found there.
 *       A loud hull is cheap to sweep, a quiet one expensive, and known-empty water is
 *       expensive because it leads nowhere.</li>
 *   <li>{@code intel} - what firing at a tile revealed. Built up only from shots actually
 *       taken, reported back through {@link #reportResults}.</li>
 * </ul>
 *
 * <p>The board is treated as a weighted graph: tiles are nodes, adjacent tiles are edges, and
 * entering a tile costs its detection weight. Target selection is then a shortest-path
 * problem, solved with Dijkstra over a {@link PriorityQueue}.
 *
 * <h2>Two modes</h2>
 * <b>Hunt</b> - with an unfinished contact on the board, it runs a multi-source search from
 * every hit whose ship is not yet sunk and works outward in increasing cost. Tiles that
 * continue a line of two or more hits are strongly preferred, because ships are straight.
 *
 * <p><b>Sweep</b> - with no live contact, it sweeps for one on one colour of a checkerboard:
 * every ship is at least two tiles long, so a checkerboard cannot be crossed without being
 * hit, and that halves the work.
 *
 * <p><b>Re-fire</b> - hits on a crippled or fully-hit ship can be fired on again at half
 * damage, as the player can. While hunting it takes those after any tile that extends a line
 * of hits, and before guessing anywhere else.
 *
 * <h2>What it is not allowed to know</h2>
 * A board's {@link StealthMap} is built from the ships on it, so a full copy of it <em>is</em>
 * the fleet layout. This opponent therefore never reads a tile it has not fired at: detection
 * only enters the search once a shot has already revealed it. That keeps the pressure on
 * finishing off a contact - which is where detection matters - without handing over the
 * answer up front.
 */
public class EliteEnemy implements EnemyAI {

    /** Stand-in for "unreachable" that still sorts sanely. */
    private static final int FAR = 1_000_000;

    /** How strongly a tile continuing a known line of hits is favoured. */
    private static final int LINE_BONUS = 500;

    /** Penalty for a tile on the off-parity squares during a sweep. */
    private static final int PARITY_PENALTY = 6;

    /** Sweep cost assumed for a tile that has not been fired at yet. */
    private static final int UNKNOWN_WEIGHT = 4;

    private final int size;

    /**
     * Sweep cost per tile.
     *
     * <p>Filled in only as tiles are revealed. Copying the whole detection map up front would
     * be cheating outright: detection values come from ships, so the full map <em>is</em> the
     * fleet layout, and the opponent would simply shoot every non-zero tile.
     */
    private final Map<Point, Integer> detectionWeight = new HashMap<>();

    /** What has been learned about each tile fired at. */
    private final Map<Point, Contact> intel = new HashMap<>();

    public EliteEnemy(int size) {
        this.size = size;
    }

    @Override
    public String getName() {
        return "Elite";
    }

    /** One tile's worth of knowledge. */
    private static final class Contact {
        final boolean hit;
        boolean resolved; // the ship on this tile has been sunk

        Contact(boolean hit) {
            this.hit = hit;
        }
    }

    /** A node on the Dijkstra frontier. Carries its own cost so stale entries can be skipped. */
    private static final class Node {
        final Point tile;
        final int cost;

        Node(Point tile, int cost) {
            this.tile = tile;
            this.cost = cost;
        }
    }

    // =====================================================================================
    // Target selection
    // =====================================================================================

    @Override
    public List<Point> generateShots(int maxShots) {
        return generateShots(maxShots, null);
    }

    /**
     * Picks a salvo, taking re-fires when they are worth it.
     *
     * <p>While hunting, a tile that extends a known line of hits comes first - it is the
     * likeliest full-damage hit. Next come the re-fire targets: a guaranteed half-damage hit
     * on a ship that is crippled, or has nowhere left to be found, beats guessing around it.
     * Only then the rest of the hunt, and a sweep when there is no live contact at all.
     */
    @Override
    public List<Point> generateShots(int maxShots, List<Point> refireTargets) {
        List<Point> untried = untriedTiles();
        List<Point> refires = new ArrayList<>();
        if (refireTargets != null) {
            for (Point p : refireTargets) {
                if (!refires.contains(p)) refires.add(new Point(p));
            }
        }
        if (untried.isEmpty() && refires.isEmpty()) return new ArrayList<>();

        List<Point> seeds = unresolvedHits();
        List<Point> ranked = new ArrayList<>();
        if (seeds.isEmpty()) {
            ranked.addAll(rankForSweep(untried));
            ranked.addAll(refires);
        } else {
            List<Point> hunt = rankForHunt(untried, seeds);
            for (Point p : hunt) {
                if (extendsKnownLine(p)) ranked.add(p);
            }
            ranked.addAll(refires);
            for (Point p : hunt) {
                if (!extendsKnownLine(p)) ranked.add(p);
            }
        }

        List<Point> chosen = new ArrayList<>();
        for (Point p : ranked) {
            if (chosen.size() >= maxShots) break;
            chosen.add(p);
        }

        // Fired tiles are recorded immediately: a salvo must not contain the same tile twice,
        // and reportResults may never arrive if the battle ends on this salvo.
        for (Point p : chosen) {
            intel.putIfAbsent(p, new Contact(false));
        }
        return chosen;
    }

    @Override
    public void reportResults(List<ShotOutcome> outcomes) {
        if (outcomes == null) return;

        for (ShotOutcome outcome : outcomes) {
            Point tile = outcome.getTile();
            Contact contact = new Contact(outcome.isHit());
            intel.put(tile, contact);

            // Now that the tile is revealed, its true sweep cost can be recorded. Detection
            // is inverted: a loud hull is cheap to follow, a quiet one is expensive, and a
            // confirmed miss is the most expensive of all because it leads nowhere.
            int detection = outcome.isHit() ? outcome.getConcealment() : StealthMap.OPEN_WATER;
            detectionWeight.put(tile, Math.max(1, StealthMap.MAX_DETECTION - detection));

            if (!outcome.isHit()) continue;

            if (outcome.isSunk()) {
                // The whole contact is finished; stop hunting around it and anything adjacent
                // that was part of the same chain.
                contact.resolved = true;
                resolveChain(tile);
            }
        }
    }

    /**
     * Marks a sunk ship's hits as finished.
     *
     * <p>Only the tile that took the killing shot is reported as sunk, so the rest of that
     * hull's hits have to be walked from it - otherwise the hunt keeps circling a wreck
     * instead of moving on.
     */
    private void resolveChain(Point start) {
        Set<Point> seen = new HashSet<>();
        List<Point> stack = new ArrayList<>();
        stack.add(start);

        while (!stack.isEmpty()) {
            Point current = stack.remove(stack.size() - 1);
            if (!seen.add(current)) continue;

            Contact contact = intel.get(current);
            if (contact == null || !contact.hit) continue;
            contact.resolved = true;

            for (Point next : neighbours(current)) {
                Contact adjacent = intel.get(next);
                if (adjacent != null && adjacent.hit && !adjacent.resolved) {
                    stack.add(next);
                }
            }
        }
    }

    // =====================================================================================
    // Hunt mode
    // =====================================================================================

    /** Orders candidates by search cost from the nearest live contact, cheapest first. */
    private List<Point> rankForHunt(List<Point> candidates, List<Point> seeds) {
        Map<Point, Integer> distance = dijkstra(seeds);

        List<Point> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator.comparingInt(p -> huntScore(p, distance)));
        return ranked;
    }

    private int huntScore(Point tile, Map<Point, Integer> distance) {
        int score = distance.getOrDefault(tile, FAR);
        if (extendsKnownLine(tile)) score -= LINE_BONUS;
        return score;
    }

    /**
     * Multi-source Dijkstra from every live contact.
     *
     * <p>Entering a tile costs its detection weight plus any concealment revealed there, so
     * the search spreads through open water before it pushes into deep water.
     */
    private Map<Point, Integer> dijkstra(List<Point> seeds) {
        Map<Point, Integer> distance = new HashMap<>();
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));

        for (Point seed : seeds) {
            distance.put(seed, 0);
            frontier.add(new Node(seed, 0));
        }

        while (!frontier.isEmpty()) {
            Node node = frontier.poll();
            // Skip entries left stale by a cheaper path found after they were queued.
            if (node.cost > distance.getOrDefault(node.tile, FAR)) continue;

            for (Point next : neighbours(node.tile)) {
                int step = detectionWeight.getOrDefault(next, UNKNOWN_WEIGHT);
                int candidate = node.cost + step;

                if (candidate < distance.getOrDefault(next, FAR)) {
                    distance.put(next, candidate);
                    frontier.add(new Node(next, candidate));
                }
            }
        }
        return distance;
    }

    /**
     * True when {@code tile} continues a straight run of two or more live hits.
     *
     * <p>Ships occupy a line, so once two hits line up the next shot almost certainly belongs
     * at one end of that run rather than beside it.
     */
    private boolean extendsKnownLine(Point tile) {
        int[][] axes = {{1, 0}, {0, 1}};
        for (int[] axis : axes) {
            for (int direction = -1; direction <= 1; direction += 2) {
                int dx = axis[0] * direction;
                int dy = axis[1] * direction;

                // Walk away from the candidate and count consecutive live hits.
                int run = 0;
                int x = tile.x + dx;
                int y = tile.y + dy;
                while (inBounds(x, y)) {
                    Contact contact = intel.get(new Point(x, y));
                    if (contact == null || !contact.hit || contact.resolved) break;
                    run++;
                    x += dx;
                    y += dy;
                }
                if (run >= 2) return true;
            }
        }
        return false;
    }

    // =====================================================================================
    // Sweep mode
    // =====================================================================================

    /** Orders candidates for a cold search: exposed water first, on one parity. */
    private List<Point> rankForSweep(List<Point> candidates) {
        List<Point> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator.comparingInt(this::sweepScore));
        return ranked;
    }

    private int sweepScore(Point tile) {
        int score = detectionWeight.getOrDefault(tile, UNKNOWN_WEIGHT);

        // Every ship is at least two tiles long, so one colour of the board is enough to
        // find all of them. Checking the other colour first is wasted effort.
        if (((tile.x + tile.y) & 1) != 0) score += PARITY_PENALTY;

        // Prefer open areas: a tile hemmed in by spent shots has less room to hide a hull.
        int room = 0;
        for (Point next : neighbours(tile)) {
            if (!intel.containsKey(next)) room++;
        }
        score -= room;

        return score;
    }

    // =====================================================================================
    // Board helpers
    // =====================================================================================

    private List<Point> untriedTiles() {
        List<Point> out = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Point p = new Point(c, r);
                if (!intel.containsKey(p)) out.add(p);
            }
        }
        return out;
    }

    private List<Point> unresolvedHits() {
        List<Point> out = new ArrayList<>();
        for (Map.Entry<Point, Contact> entry : intel.entrySet()) {
            Contact contact = entry.getValue();
            if (contact.hit && !contact.resolved) out.add(entry.getKey());
        }
        return out;
    }

    private List<Point> neighbours(Point tile) {
        List<Point> out = new ArrayList<>(4);
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] step : steps) {
            int x = tile.x + step[0];
            int y = tile.y + step[1];
            if (inBounds(x, y)) out.add(new Point(x, y));
        }
        return out;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }
}
