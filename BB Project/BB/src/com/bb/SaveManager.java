package com.bb;

import Ships.Ship_Placement;
import Ships.Ships_Type;
import Ships.Submarine;
import skills.Skills;
import skills.SkillsRegistry;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes a whole run to disk.
 *
 * <p>The format is a plain UTF-8 text file of {@code key=value} lines rather than Java
 * serialization. That keeps saves readable and, more usefully, keeps them working when a
 * ship or skill class is edited - a serialized object graph would break on the next change
 * to any of those classes.
 *
 * <p>A save captures the run (stage, score, salvo size, loadout) <em>and</em> the battle in
 * progress (both boards, both sets of hull points, every tile already fired at), so loading
 * puts the player back exactly where they stopped.
 */
public final class SaveManager {

    private static final String MAGIC = "BBSAVE";
    private static final int VERSION = 1;
    private static final int SLOTS = 3;

    private SaveManager() {}

    public static int slotCount() {
        return SLOTS;
    }

    private static Path saveDir() {
        return Paths.get(System.getProperty("user.home"), ".battleship-bb");
    }

    public static Path slotPath(int slot) {
        return saveDir().resolve("slot" + slot + ".bbs");
    }

    public static boolean slotExists(int slot) {
        return Files.isRegularFile(slotPath(slot));
    }

    /** One-line summary of a slot, for menus. */
    public static String describeSlot(int slot) {
        if (!slotExists(slot)) return "Slot " + slot + " - empty";
        try {
            int stage = 1;
            int score = 0;
            for (String line : Files.readAllLines(slotPath(slot), StandardCharsets.UTF_8)) {
                if (line.startsWith("stage=")) stage = parseInt(line.substring(6), 1);
                if (line.startsWith("score=")) score = parseInt(line.substring(6), 0);
            }
            return "Slot " + slot + " - stage " + stage + ", score " + score;
        } catch (IOException e) {
            return "Slot " + slot + " - unreadable";
        }
    }

    // =====================================================================================
    // Writing
    // =====================================================================================

    /**
     * Writes the current run and battle to {@code slot}.
     *
     * @return null on success, or a message explaining why the save failed
     */
    public static String save(int slot, GameLayout player, OpponentPanel opponent) {
        RunState run = RunState.current();
        StringBuilder sb = new StringBuilder();

        sb.append(MAGIC).append('=').append(VERSION).append('\n');
        sb.append("stage=").append(run.getStage()).append('\n');
        sb.append("score=").append(run.getScore()).append('\n');
        sb.append("stagesCleared=").append(run.getStagesCleared()).append('\n');
        sb.append("baseShots=").append(run.getBaseShots()).append('\n');

        List<String> skillNames = new ArrayList<>();
        for (Skills s : run.getLoadout()) {
            skillNames.add(s.getClass().getName());
        }
        sb.append("skills=").append(String.join("|", skillNames)).append('\n');

        // Player fleet: type, originX, originY, horizontal, currentHP, maxHP, oxygen, surfaced.
        // Undeployed ships are written with an origin of -1,-1 so the roster survives too.
        // The last two fields only mean anything for submarines; other hulls write -1/false.
        for (Ship_Placement sp : player.getFleet().getPlacements()) {
            Point o = sp.getOrigin();
            boolean isSub = sp.getShip() instanceof Submarine;
            Submarine sub = isSub ? (Submarine) sp.getShip() : null;

            sb.append("player=")
              .append(sp.getShip().getClass().getName()).append(',')
              .append(o == null ? -1 : o.x).append(',')
              .append(o == null ? -1 : o.y).append(',')
              .append(sp.isHorizontal()).append(',')
              .append(o == null ? 0 : player.getShipHP(sp)).append(',')
              .append(o == null ? 0 : player.getShipMaxHP(sp)).append(',')
              .append(isSub ? sub.getOxygen() : -1).append(',')
              .append(isSub && sub.isSurfaced())
              .append('\n');
        }

        sb.append("playerIncoming=")
          .append(encodeShots(player.getIncomingShots(), player.getIncomingHits()))
          .append('\n');

        // The detection map is derived from the fleet, so it is not saved: rebuilding it from
        // the restored ships gives exactly the same grid, and storing it would only create a
        // second source of truth that could drift.

        for (Ship_Placement sp : opponent.getOpponentPlacements()) {
            Point o = sp.getOrigin();
            if (o == null) continue;
            sb.append("enemy=")
              .append(sp.getShip().getClass().getName()).append(',')
              .append(o.x).append(',')
              .append(o.y).append(',')
              .append(sp.isHorizontal()).append(',')
              .append(opponent.getOpponentShipHP(sp))
              .append('\n');
        }

        sb.append("enemyFired=").append(encodeShots(opponent.getFiredOnOpponent(), null)).append('\n');

        try {
            Files.createDirectories(saveDir());
            try (BufferedWriter w = Files.newBufferedWriter(slotPath(slot), StandardCharsets.UTF_8)) {
                w.write(sb.toString());
            }
            return null;
        } catch (IOException e) {
            return "Could not write the save file: " + e.getMessage();
        }
    }

    /** Encodes a boolean grid as {@code r:c:hit} triples, or {@code r:c} pairs when hits is null. */
    private static String encodeShots(boolean[][] shots, boolean[][] hits) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < shots.length; r++) {
            for (int c = 0; c < shots[r].length; c++) {
                if (!shots[r][c]) continue;
                if (sb.length() > 0) sb.append('|');
                sb.append(r).append(':').append(c);
                if (hits != null) sb.append(':').append(hits[r][c]);
            }
        }
        return sb.toString();
    }

    // =====================================================================================
    // Reading
    // =====================================================================================

    /**
     * Restores a run and its battle from {@code slot}.
     *
     * @return null on success, or a message explaining why the load failed
     */
    public static String load(int slot, GameLayout player, OpponentPanel opponent) {
        Path path = slotPath(slot);
        if (!Files.isRegularFile(path)) return "Slot " + slot + " is empty.";

        List<String> lines;
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            lines = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        } catch (IOException e) {
            return "Could not read the save file: " + e.getMessage();
        }

        if (lines.isEmpty() || !lines.get(0).startsWith(MAGIC + "=")) {
            return "That file is not a Battleship save.";
        }

        RunState run = new RunState();
        List<Skills> loadout = new ArrayList<>();
        List<Ship_Placement> playerShips = new ArrayList<>();
        List<int[]> playerHulls = new ArrayList<>();   // {currentHP, maxHP}
        List<Ship_Placement> enemyShips = new ArrayList<>();
        List<Integer> enemyHulls = new ArrayList<>();
        String playerIncoming = "";
        String enemyFired = "";

        try {
            for (String line : lines) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq);
                String value = line.substring(eq + 1);

                switch (key) {
                    case "stage":         run.setStage(parseInt(value, 1)); break;
                    case "score":         run.setScore(parseInt(value, 0)); break;
                    case "stagesCleared": run.setStagesCleared(parseInt(value, 0)); break;
                    case "baseShots":     run.setBaseShots(parseInt(value, RunState.STARTING_SHOTS)); break;
                    case "skills":
                        for (String name : splitList(value)) {
                            Skills s = instantiateSkill(name);
                            if (s != null) loadout.add(s);
                        }
                        break;
                    case "player": {
                        String[] f = value.split(",");
                        if (f.length < 6) break;
                        Ships_Type type = instantiateShip(f[0]);
                        if (type == null) break;
                        int x = parseInt(f[1], -1);
                        int y = parseInt(f[2], -1);
                        boolean horizontal = Boolean.parseBoolean(f[3]);
                        Point origin = (x < 0 || y < 0) ? null : new Point(x, y);

                        // Oxygen fields are optional, so saves written before submarines
                        // gained them still load; the boat just starts with a full tank.
                        if (type instanceof Submarine && f.length >= 8) {
                            int oxygen = parseInt(f[6], -1);
                            if (oxygen >= 0) {
                                ((Submarine) type).restoreOxygen(oxygen, Boolean.parseBoolean(f[7]));
                            }
                        }

                        playerShips.add(new Ship_Placement(type, origin, horizontal));
                        playerHulls.add(new int[]{parseInt(f[4], type.getHP()), parseInt(f[5], type.getHP())});
                        break;
                    }
                    case "playerIncoming": playerIncoming = value; break;
                    case "enemy": {
                        String[] f = value.split(",");
                        if (f.length < 5) break;
                        Ships_Type type = instantiateShip(f[0]);
                        if (type == null) break;
                        enemyShips.add(new Ship_Placement(type,
                                new Point(parseInt(f[1], 0), parseInt(f[2], 0)),
                                Boolean.parseBoolean(f[3])));
                        enemyHulls.add(parseInt(f[4], type.getHP()));
                        break;
                    }
                    case "enemyFired": enemyFired = value; break;
                    default: break;
                }
            }
        } catch (RuntimeException e) {
            return "That save file is corrupted: " + e.getMessage();
        }

        if (playerShips.isEmpty()) return "That save file has no fleet in it.";

        // Everything parsed, so it is safe to start mutating live state.
        run.setLoadout(loadout);
        RunState.restore(run);
        SkillsRegistry.setSelectedSkills(loadout);

        player.resetBoard();

        player.getFleet().setPlacements(playerShips);
        player.rebuildRosterFromFleet();
        for (int i = 0; i < playerShips.size(); i++) {
            Ship_Placement sp = playerShips.get(i);
            if (sp.getOrigin() == null) continue;
            int[] hull = playerHulls.get(i);
            player.restoreShip(sp, hull[0], hull[1]);
        }
        for (String token : splitList(playerIncoming)) {
            String[] f = token.split(":");
            if (f.length < 2) continue;
            player.restoreIncoming(parseInt(f[0], -1), parseInt(f[1], -1),
                    f.length > 2 && Boolean.parseBoolean(f[2]));
        }
        player.refreshAfterLoad();

        boolean[][] fired = new boolean[GameLayout.SIZE][GameLayout.SIZE];
        for (String token : splitList(enemyFired)) {
            String[] f = token.split(":");
            if (f.length < 2) continue;
            int r = parseInt(f[0], -1);
            int c = parseInt(f[1], -1);
            if (r >= 0 && r < fired.length && c >= 0 && c < fired.length) fired[r][c] = true;
        }
        opponent.restoreStage(enemyShips, enemyHulls, fired);

        return null;
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static List<String> splitList(String value) {
        List<String> out = new ArrayList<>();
        if (value == null || value.isEmpty()) return out;
        for (String part : value.split("\\|")) {
            if (!part.isEmpty()) out.add(part);
        }
        return out;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static Ships_Type instantiateShip(String className) {
        try {
            Class<?> cls = Class.forName(className);
            if (!Ships_Type.class.isAssignableFrom(cls)) return null;
            return (Ships_Type) cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Unknown ship class in save: " + className);
            return null;
        }
    }

    private static Skills instantiateSkill(String className) {
        try {
            Class<?> cls = Class.forName(className);
            if (!Skills.class.isAssignableFrom(cls)) return null;
            return (Skills) cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Unknown skill class in save: " + className);
            return null;
        }
    }
}
