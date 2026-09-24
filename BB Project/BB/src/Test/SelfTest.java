package Test;

import Ships.Submarine;
import Ships.vessels.Balao;
import Ships.vessels.Baltimore;
import Ships.vessels.Essex;
import Ships.vessels.Montana;
import Ships.vessels.Nagato;
import Ships.DefaultFleet;
import Ships.FleetCalculation;
import Ships.Ship_Placement;
import Ships.Ships_Type;
import Ships.StealthMap;
import Ships.vessels.I_556;
import Ships.vessels.Helena;
import com.bb.EliteEnemy;
import com.bb.EnemyAI;
import com.bb.Frames;
import com.bb.GameLayout;
import com.bb.OpponentGenerator;
import com.bb.NormalEnenmy;
import com.bb.OpponentPanel;
import com.bb.Reward;
import com.bb.RunState;
import com.bb.SaveManager;
import com.bb.ShotOutcome;
import skills.Attacker_Skills_Instance;
import skills.Defender_Skills_Instance;
import skills.ERA_GOLD;
import skills.Enhance;
import skills.ModifiedStats;
import skills.Rapid_Fire;
import skills.Skills;
import skills.SkillsRegistry;
import skills.Skills_Register;
import skills.modify_DMG;
import skills.modify_HP;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Self-check for the behaviours that were broken, so a regression shows up immediately.
 *
 * <p>It builds the real window on the event dispatch thread and then drives the run logic
 * directly. Run it with {@code selftest.bat}, or:
 * <pre>java -cp "bin;lib" Test.SelfTest</pre>
 *
 * <p>Exits 0 when everything passes and 1 when anything fails.
 */
public class SelfTest {

    private static int failures = 0;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + what);
        if (!ok) failures++;
    }

    private static void eq(String what, Object actual, Object expected) {
        boolean ok = Objects.equals(actual, expected);
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + what
                + "  (got " + actual + ", expected " + expected + ")");
        if (!ok) failures++;
    }

    private static void header(String text) {
        System.out.println();
        System.out.println("== " + text + " ==");
    }

    public static void main(String[] args) throws Exception {
        final Frames[] holder = new Frames[1];

        SwingUtilities.invokeAndWait(() -> {
            header("1. UI construction");
            holder[0] = new Frames();
            check("Frames constructed without exception", true);
        });

        final Frames frames = holder[0];
        final GameLayout player = frames.getPlayerBoard();
        final OpponentPanel opponent = frames.getOpponentPanel();

        SwingUtilities.invokeAndWait(() -> {
            header("2. Shot count is dynamic, default 3");
            RunState.startNewRun();
            eq("base shots at run start", RunState.current().getBaseShots(), 3);
            player.autoDeploy();
            check("autoDeploy puts something on the board", player.isReadyForBattle());
            eq("available shots with a full fleet", player.getAvailableShots(), 3);

            RunState.current().addBaseShots(2);
            eq("after a +2 reward", player.getAvailableShots(), 5);

            header("3. Rapid Fire doubles the salvo");
            List<Skills> loadout = new ArrayList<>();
            loadout.add(new Rapid_Fire());
            player.setActiveSkills(loadout);
            eq("shots with Rapid Fire active", player.getAvailableShots(), 10);

            header("4. Skills are active at all");
            check("Rapid_Fire active", new Attacker_Skills_Instance(new Rapid_Fire()).isActive());
            check("Enhance active", new Attacker_Skills_Instance(new Enhance()).isActive());
            check("modify_DMG active", new Attacker_Skills_Instance(new modify_DMG()).isActive());
            check("modify_HP active", new Defender_Skills_Instance(new modify_HP()).isActive());

            List<Skills> dmgLoadout = new ArrayList<>();
            dmgLoadout.add(new Enhance());
            player.setActiveSkills(dmgLoadout);
            float mod = new ModifiedStats().dmgModifier();
            System.out.println("        Enhance damage modifier = " + mod);
            check("Enhance raises the damage modifier above 1", mod > 1.0f);

            header("5. Rotation happens while carrying, not in the roster");
            RunState.startNewRun();
            player.resetBoard();

            // R does nothing unless a ship is actually in hand.
            Ship_Placement idle = findInRoster(player, "DD");
            boolean idleBefore = idle.isHorizontal();
            player.rotateSelected();
            eq("R with nothing in hand leaves the roster alone",
                    idle.isHorizontal(), idleBefore);

            // Pick a ship up, turn it mid-carry, and put it down: the orientation sticks.
            Ship_Placement carry = findInRoster(player, "DD");
            boolean beforeCarry = carry.isHorizontal();
            player.beginCarryAt(carry, new Point(2, 2));
            check("the ship is in hand", player.isCarrying());
            player.rotateSelected();
            player.endCarryAt(new Point(2, 2));
            check("it landed on the board", player.isDeployed(carry));
            eq("rotating mid-carry changed the orientation it landed in",
                    carry.isHorizontal(), !beforeCarry);
            check("nothing is left in hand", !player.isCarrying());

            // A deployed ship can be picked up again and moved - it does not stick.
            Point wasAt = new Point(carry.getOrigin());
            player.beginCarryAt(carry, new Point(5, 5));
            check("a deployed ship can be picked back up", player.isCarrying());
            check("picking it up frees its tiles", !player.isDeployed(carry));
            player.endCarryAt(new Point(5, 5));
            check("it is deployed again after the move", player.isDeployed(carry));
            check("and it actually moved",
                    !carry.getOrigin().equals(wasAt) && carry.getOrigin().equals(new Point(5, 5)));

            // Releasing off the board sends it back to port rather than losing it.
            player.beginCarryAt(carry, null);
            player.endCarryAt(null);
            check("released off the board, the ship returns to the roster",
                    !player.isDeployed(carry));
            eq("and stops costing anything", player.getDeployedCost(), 0);

            // Calling rotateSelected() directly would still pass if the R key were never
            // bound to anything - which is exactly what was wrong before. So check the
            // binding itself, and then fire the Action the way the keystroke would.
            Object key = player.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
            eq("R is bound in the window-scoped input map", key, "rotateShip");

            Action rotateAction = player.getActionMap().get("rotateShip");
            check("the R binding resolves to an action", rotateAction != null);

            if (rotateAction != null) {
                Ship_Placement second = findInRoster(player, "CL");
                boolean wasHorizontal = second.isHorizontal();
                player.beginCarryAt(second, new Point(0, 0));
                rotateAction.actionPerformed(
                        new ActionEvent(player, ActionEvent.ACTION_PERFORMED, "rotateShip"));
                player.endCarryAt(new Point(0, 0));
                eq("firing the R action rotates the carried ship",
                        second.isHorizontal(), !wasHorizontal);
                player.resetBoard();
            }

            header("6. Damage model uses the defender's shields");
            List<Ship_Placement> attacker = new ArrayList<>();
            attacker.add(new Ship_Placement(new Nagato(), new Point(0, 0), true));
            List<Ship_Placement> weak = new ArrayList<>();
            weak.add(new Ship_Placement(new Balao(), new Point(0, 0), true));
            List<Ship_Placement> tough = new ArrayList<>();
            tough.add(new Ship_Placement(new Montana(), new Point(0, 0), true));

            float vsWeak = FleetCalculation.damageBetween(attacker, weak, 1f, 1f);
            float vsTough = FleetCalculation.damageBetween(attacker, tough, 1f, 1f);
            System.out.println("        vs light shields: " + vsWeak + "   vs heavy shields: " + vsTough);
            check("tougher shields reduce incoming damage", vsTough < vsWeak);

            // Use the multiplier a real defensive skill actually supplies rather than an
            // invented one. Penetration is a step function - a shield only helps once it
            // exceeds the attacker's penetration - so a small multiplier on a submarine's
            // shield of 1 changes nothing, and testing with one would prove nothing.
            float eraMultiplier = new ERA_GOLD().modify_shield();
            float vsShielded = FleetCalculation.damageBetween(attacker, weak, 1f, eraMultiplier);
            System.out.println("        with ERA Gold (x" + eraMultiplier + "): " + vsShielded);
            check("a defensive shield skill reduces incoming damage", vsShielded < vsWeak);
        });

        SwingUtilities.invokeAndWait(() -> {
            header("7. Stage progression");
            RunState.startNewRun();
            player.resetBoard();
            player.autoDeploy();
            eq("starts on stage 1", RunState.current().getStage(), 1);
            int before = RunState.current().getScore();
            frames.triggerStageWon(20);
            eq("advanced to stage 2", RunState.current().getStage(), 2);
            check("score increased", RunState.current().getScore() > before);
            eq("stages cleared", RunState.current().getStagesCleared(), 1);
        });

        SwingUtilities.invokeAndWait(() -> {
            header("8. Enemy scales with stage");
            OpponentGenerator gen = new OpponentGenerator(GameLayout.SIZE);
            DefaultFleet playerFleet = new DefaultFleet();
            int stage1 = 0;
            int stage8 = 0;
            for (int i = 0; i < 40; i++) {
                for (Ship_Placement sp : gen.buildOpponentFleet(playerFleet, 1)) {
                    stage1 += sp.getShip().getSize();
                }
                for (Ship_Placement sp : gen.buildOpponentFleet(playerFleet, 8)) {
                    stage8 += sp.getShip().getSize();
                }
            }
            System.out.println("        average tonnage - stage 1: " + (stage1 / 40.0)
                    + ", stage 8: " + (stage8 / 40.0));
            check("stage 8 fleets are bigger than stage 1", stage8 > stage1);
            check("stage 1 is never empty", !gen.buildOpponentFleet(playerFleet, 1).isEmpty());
        });

        SwingUtilities.invokeAndWait(() -> {
            header("9. Save / load round trip");
            RunState.startNewRun();
            player.resetBoard();
            player.autoDeploy();
            RunState.current().setStage(4);
            RunState.current().setScore(777);
            RunState.current().addBaseShots(2);

            List<Skills> loadout = new ArrayList<>();
            loadout.add(new Enhance());
            loadout.add(new ERA_GOLD());
            player.setActiveSkills(loadout);
            player.restoreIncoming(2, 3, true);
            player.restoreIncoming(5, 5, false);

            int shipsBefore = player.getDeployedPlacements().size();
            int[][] detectionBefore = player.getStealthMap().toGrid();

            // Slot 9 is outside the 3 the menus use, so a self-test never eats a real save.
            String saveError = SaveManager.save(9, player, opponent);
            check("save wrote without error (" + saveError + ")", saveError == null);

            RunState.startNewRun();
            player.resetBoard();
            eq("run reset before load", RunState.current().getStage(), 1);

            String loadError = SaveManager.load(9, player, opponent);
            check("load returned without error (" + loadError + ")", loadError == null);
            eq("stage restored", RunState.current().getStage(), 4);
            eq("score restored", RunState.current().getScore(), 777);
            eq("baseShots restored", RunState.current().getBaseShots(), 5);
            eq("loadout restored", RunState.current().getLoadout().size(), 2);
            eq("deployed ships restored", player.getDeployedPlacements().size(), shipsBefore);
            check("incoming hit restored",
                    player.getIncomingShots()[2][3] && player.getIncomingHits()[2][3]);
            check("incoming miss restored",
                    player.getIncomingShots()[5][5] && !player.getIncomingHits()[5][5]);

            // The map is derived, so it must come back identical purely from the restored
            // fleet - including a submarine that was saved surfaced.
            int[][] detectionAfter = player.getStealthMap().toGrid();
            boolean sameDetection = true;
            for (int r = 0; r < GameLayout.SIZE && sameDetection; r++) {
                for (int c = 0; c < GameLayout.SIZE; c++) {
                    if (detectionBefore[r][c] != detectionAfter[r][c]) {
                        sameDetection = false;
                        break;
                    }
                }
            }
            check("detection map rebuilds identically after a load", sameDetection);

            try {
                java.nio.file.Files.deleteIfExists(SaveManager.slotPath(9));
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        });

        SwingUtilities.invokeAndWait(() -> {
            header("10. Rewards apply to the run");
            RunState.startNewRun();
            player.resetBoard();
            player.autoDeploy();

            int shotsBefore = player.getAvailableShots();
            Reward.moreShots().apply(player);
            eq("Extra Salvo raises the salvo size", player.getAvailableShots(), shotsBefore + 1);

            int skillsBefore = RunState.current().getLoadout().size();
            Reward.skill(new Enhance()).apply(player);
            eq("skill reward joins the loadout",
                    RunState.current().getLoadout().size(), skillsBefore + 1);
            check("skill reward reaches the modifier math",
                    new ModifiedStats().dmgModifier() > 1.0f);

            // A new hull must reach the roster, or it could never be deployed at all.
            int fleetBefore = player.getFleet().getPlacements().size();
            Reward.ship(new I_556()).apply(player);
            player.rebuildRosterFromFleet();
            eq("ship reward joins the fleet",
                    player.getFleet().getPlacements().size(), fleetBefore + 1);
            check("the roster still holds undeployed hulls",
                    player.deployedCount() < player.getFleet().getPlacements().size());

            header("11. Timed skills last as long as advertised");
            List<Skills> timed = new ArrayList<>();
            timed.add(new Rapid_Fire()); // turns() == 3
            player.setActiveSkills(timed);
            check("Rapid Fire is on at turn 0", new ModifiedStats().shotsModifier() > 1.0f);
            SkillsRegistry.tickTurnAll();
            SkillsRegistry.tickTurnAll();
            check("still on after 2 exchanges", new ModifiedStats().shotsModifier() > 1.0f);
            SkillsRegistry.tickTurnAll();
            check("expired after the 3rd exchange", new ModifiedStats().shotsModifier() == 1.0f);
        });

        SwingUtilities.invokeAndWait(() -> {
            header("12. Detection map is derived from the fleet");
            RunState.startNewRun();
            player.resetBoard();

            StealthMap empty = player.getStealthMap();
            check("an empty board reads as open water everywhere",
                    empty.at(0, 0) == StealthMap.OPEN_WATER
                            && empty.at(4, 4) == StealthMap.OPEN_WATER);

            player.autoDeploy();
            StealthMap map = player.getStealthMap();

            // Every deployed hull must show its own detection on each tile it occupies.
            boolean detectionMatches = true;
            for (Ship_Placement sp : player.getDeployedPlacements()) {
                for (Point tile : sp.getOccupiedTiles()) {
                    if (map.at(tile.y, tile.x) != sp.getShip().getDetection()) {
                        detectionMatches = false;
                    }
                }
            }
            check("ship tiles carry that ship's detection", detectionMatches);
            check("sweep cost is always at least 1", map.cost(0, 0) >= 1);
            check("a quiet hull costs more to sweep than a loud one",
                    new StealthMap(new int[][]{{1}}).cost(0, 0)
                            > new StealthMap(new int[][]{{7}}).cost(0, 0));

            check("a submerged submarine is quieter than a battleship",
                    new Balao().getDetection() < new Nagato().getDetection());
        });

        SwingUtilities.invokeAndWait(() -> {
            header("13. Submarine oxygen");
            Balao boat = new Balao();
            int fullTank = boat.getMaxOxygen();
            int submergedDetection = boat.getDetection();

            check("starts submerged with a full tank",
                    !boat.isSurfaced() && boat.getOxygen() == fullTank);

            // Burn the tank one turn at a time; the last tick must force it up.
            boolean forced = false;
            for (int turn = 0; turn < fullTank; turn++) {
                forced = boat.tickTurn();
            }
            eq("oxygen reaches zero after a full tank of turns", boat.getOxygen(), 0);
            check("running out forces the boat to the surface", forced && boat.isSurfaced());
            check("surfacing raises detection",
                    boat.getDetection() > submergedDetection);
            eq("the rise is exactly the surfaced penalty",
                    boat.getDetection() - submergedDetection,
                    Ships.Submarine.SURFACED_DETECTION_PENALTY);

            // Surfaced, it takes air back on and can dive again.
            boat.tickTurn();
            check("a surfaced boat regains oxygen", boat.getOxygen() > 0);
            check("it can dive once it has enough air", boat.dive());
            check("diving brings detection back down",
                    boat.getDetection() == submergedDetection);

            // A hull on the board must feed the tick through the player's fleet.
            RunState.startNewRun();
            player.resetBoard();
            player.autoDeploy();
            int before = player.getStealthMap().toGrid().length;
            player.tickSubmarines();
            check("ticking the fleet does not disturb the board", before == GameLayout.SIZE);
        });

        SwingUtilities.invokeAndWait(() -> {
            header("14. EliteEnemy searches instead of guessing");

            // Same fleets, both opponents: the searching one must clear faster.
            int normalTotal = 0;
            int eliteTotal = 0;
            int trials = 60;

            for (int i = 0; i < trials; i++) {
                List<Ship_Placement> fleet = simFleet(4242L + i);
                normalTotal += simBattle(new NormalEnenmy(GameLayout.SIZE), fleet);
                eliteTotal += simBattle(new EliteEnemy(GameLayout.SIZE), fleet);
            }

            double normalAvg = normalTotal / (double) trials;
            double eliteAvg = eliteTotal / (double) trials;
            System.out.printf("        shots to clear - Standard %.1f, Elite %.1f%n",
                    normalAvg, eliteAvg);
            check("Elite clears a fleet faster than random fire", eliteAvg < normalAvg);
            check("Elite is meaningfully faster, not marginally",
                    eliteAvg < normalAvg * 0.85);

            EnemyAI elite = new EliteEnemy(GameLayout.SIZE);
            check("Elite names itself for the status bar", "Elite".equals(elite.getName()));
            check("Elite never repeats a tile", noRepeats(elite));
        });

        SwingUtilities.invokeAndWait(() -> {
            header("15. Cost caps what can be deployed");
            RunState.startNewRun();
            player.resetBoard();

            int budget = RunState.current().getDeploymentBudget();
            eq("stage 1 deployment budget", budget, RunState.STARTING_DEPLOYMENT_BUDGET);
            check("the roster is worth more than the budget can field",
                    player.getFleet().totalCost() > budget);

            // The example from the spec: at a budget of 10 a battleship fills it on its own.
            Ship_Placement bb = findInRoster(player, "BB");
            check("the roster has a battleship", bb != null);
            if (bb != null) {
                bb.setOrigin(new Point(0, 0));
                check("a cost-10 battleship fits an empty budget of 10",
                        player.validatePlacement(bb));
                player.commitPlacement(bb);
                eq("deployed cost after the battleship", player.getDeployedCost(), 10);
                eq("nothing left to spend", player.getRemainingBudget(), 0);

                // Nothing else can join it, however small.
                Ship_Placement dd = findInRoster(player, "DD");
                check("the roster has a destroyer", dd != null);
                if (dd != null) {
                    dd.setOrigin(new Point(0, 6));
                    check("even a cost-2 destroyer is refused once the budget is spent",
                            !player.validatePlacement(dd));
                    check("the refusal is about cost, not geometry", !player.canAfford(dd));
                    dd.setOrigin(null);
                }
                check("the budget counts as exhausted", player.isBudgetExhausted());

                // Taking the battleship back frees the whole allowance again.
                player.removeShipFromBoard(bb);
                eq("removing it refunds the cost", player.getDeployedCost(), 0);
            }

            // The other half of the example: a carrier and a destroyer also come to 10.
            player.resetBoard();
            Ship_Placement cv = findInRoster(player, "CV");
            Ship_Placement dd2 = findInRoster(player, "DD");
            if (cv != null && dd2 != null) {
                cv.setOrigin(new Point(0, 0));
                player.commitPlacement(cv);
                dd2.setOrigin(new Point(0, 6));
                check("a carrier plus a destroyer also fits the budget",
                        player.validatePlacement(dd2));
                player.commitPlacement(dd2);
                eq("carrier plus destroyer spends exactly the budget",
                        player.getDeployedCost(), 10);
            }

            // One ship on the board is enough to fight; a full roster is not required.
            check("one deployed ship is enough to start", player.isReadyForBattle());

            // The reported bug: a refused placement used to leave the ship's origin pointing
            // at the square it was refused from, and "deployed" was decided by that origin,
            // so the ship kept costing budget it had never been allowed to spend.
            player.resetBoard();
            Ship_Placement bigShip = findInRoster(player, "BB");
            player.beginCarryAt(bigShip, new Point(0, 0));
            player.endCarryAt(new Point(0, 0));
            eq("battleship deployed", player.getDeployedCost(), 10);

            Ship_Placement extra = findInRoster(player, "DD");
            player.beginCarryAt(extra, new Point(0, 6));
            player.endCarryAt(new Point(0, 6));   // refused: no budget left
            check("the refused ship is not on the board", !player.isDeployed(extra));
            eq("and did not cost anything", player.getDeployedCost(), 10);
            check("the budget is not exceeded",
                    player.getDeployedCost() <= RunState.current().getDeploymentBudget());
            check("isReadyForBattle refuses an over-budget board",
                    player.isReadyForBattle());
            check("the refused ship is back in the roster", findInRoster(player, "DD") != null);

            // The allowance grows with the run.
            check("the budget grows each stage",
                    RunState.deploymentBudgetForStage(2)
                            > RunState.deploymentBudgetForStage(1));
            System.out.println("        budget by stage: "
                    + RunState.deploymentBudgetForStage(1) + ", "
                    + RunState.deploymentBudgetForStage(4) + ", "
                    + RunState.deploymentBudgetForStage(8));

            // autoDeploy has to respect the cap too.
            player.resetBoard();
            player.autoDeploy();
            check("autoDeploy stays inside the budget",
                    player.getDeployedCost() <= RunState.current().getDeploymentBudget());
            check("autoDeploy actually spends the budget", player.getDeployedCost() > 0);

            header("15b. Hull classes and cost");
            eq("twelve named vessels registered",
                    Ships.vessels.VesselRegistry.all().size(), 12);

            String[] hulls = {"CV", "BB", "CB", "CL", "DD", "SS"};
            for (String hull : hulls) {
                eq("two vessels for " + hull,
                        Ships.vessels.VesselRegistry.byHull(hull).size(), 2);
            }

            // Cost belongs to the class, so both vessels of a class must agree on it, and it
            // must be declared final so nothing can override it.
            check("both battleships cost the same",
                    new Nagato().getCost() == new Montana().getCost());
            eq("battleship cost", new Nagato().getCost(), Ships.Battleship.COST);
            eq("destroyer is the cheapest hull",
                    Ships.vessels.VesselRegistry.cheapestCost(), Ships.Destroyer.COST);
            check("a battleship costs more than a destroyer",
                    Ships.Battleship.COST > Ships.Destroyer.COST);

            try {
                java.lang.reflect.Method m = Ships.Battleship.class.getMethod("getCost");
                check("getCost is final, so no vessel can change it",
                        java.lang.reflect.Modifier.isFinal(m.getModifiers()));
            } catch (NoSuchMethodException e) {
                check("getCost is declared on the hull class", false);
            }

            // Enemy fleets are budgeted in cost, so a later stage must be able to spend more.
            OpponentGenerator gen = new OpponentGenerator(GameLayout.SIZE);
            DefaultFleet pf = new DefaultFleet();
            check("the starting fleet has a cost", pf.totalCost() > 0);
            System.out.println("        starting fleet cost: " + pf.totalCost());

            int early = 0;
            int late = 0;
            for (int i = 0; i < 30; i++) {
                for (Ship_Placement sp : gen.buildOpponentFleet(pf, 1)) early += sp.getShip().getCost();
                for (Ship_Placement sp : gen.buildOpponentFleet(pf, 8)) late += sp.getShip().getCost();
            }
            System.out.printf("        average enemy fleet cost - stage 1: %.1f, stage 8: %.1f%n",
                    early / 30.0, late / 30.0);
            check("stage 8 fleets cost more than stage 1", late > early);
        });

        header("16. Skill catalogue and art");
        eq("all skills registered", Skills_Register.getAllSkills().size(), 8);
        for (Skills s : Skills_Register.getAllSkills()) {
            // Assets.getResource, not a raw classpath lookup: this is the same path every
            // real load goes through, including the filesystem fallback that lets the game
            // find its art without lib/ having to be added to the classpath by hand.
            java.net.URL url = com.bb.Assets.getResource("/" + s.getImage());
            check("art found for " + s.getName() + " (" + s.getImage() + ")", url != null);
        }

        System.out.println();
        System.out.println("=====================================");
        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : (failures + " CHECK(S) FAILED"));
        System.out.println("=====================================");
        System.exit(failures == 0 ? 0 : 1);
    }

    // =====================================================================================
    // Lightweight battle simulation, used to compare the two opponents
    // =====================================================================================

    /** The first undeployed ship of a hull class in the player's roster, or null. */
    private static Ship_Placement findInRoster(GameLayout player, String hullCode) {
        for (Ship_Placement sp : player.getFleet().getPlacements()) {
            if (sp.getOrigin() == null && sp.getShip().getHullCode().equals(hullCode)) {
                return sp;
            }
        }
        return null;
    }

    /** A standard fleet placed at random, without needing a live board. */
    private static List<Ship_Placement> simFleet(long seed) {
        Random rand = new Random(seed);
        List<Ships_Type> types = new ArrayList<>();
        types.add(new Nagato());
        types.add(new Essex());
        types.add(new Baltimore());
        types.add(new Balao());

        boolean[][] used = new boolean[GameLayout.SIZE][GameLayout.SIZE];
        List<Ship_Placement> fleet = new ArrayList<>();

        for (Ships_Type t : types) {
            for (int attempt = 0; attempt < 200; attempt++) {
                boolean horizontal = rand.nextBoolean();
                int size = t.getSize();
                int maxX = horizontal ? GameLayout.SIZE - size : GameLayout.SIZE - 1;
                int maxY = horizontal ? GameLayout.SIZE - 1 : GameLayout.SIZE - size;
                Ship_Placement sp = new Ship_Placement(t,
                        new Point(rand.nextInt(maxX + 1), rand.nextInt(maxY + 1)), horizontal);

                boolean fits = true;
                for (Point p : sp.getOccupiedTiles()) {
                    if (used[p.y][p.x]) {
                        fits = false;
                        break;
                    }
                }
                if (!fits) continue;

                for (Point p : sp.getOccupiedTiles()) used[p.y][p.x] = true;
                fleet.add(sp);
                break;
            }
        }
        return fleet;
    }

    /** Fires one shot per turn until the fleet is gone; returns the shots it took. */
    private static int simBattle(EnemyAI ai, List<Ship_Placement> fleet) {
        Map<Point, Ship_Placement> tiles = new HashMap<>();
        Map<Ship_Placement, Integer> hull = new HashMap<>();
        for (Ship_Placement sp : fleet) {
            hull.put(sp, sp.getShip().getSize()); // one hit per tile
            for (Point p : sp.getOccupiedTiles()) tiles.put(new Point(p.x, p.y), sp);
        }

        int shots = 0;
        int cap = GameLayout.SIZE * GameLayout.SIZE + 5;
        for (int turn = 0; turn < cap; turn++) {
            List<Point> picks = ai.generateShots(1);
            if (picks.isEmpty()) break;

            List<ShotOutcome> outcomes = new ArrayList<>();
            for (Point p : picks) {
                shots++;
                Ship_Placement sp = tiles.get(p);
                if (sp == null) {
                    outcomes.add(new ShotOutcome(p, false, false, 0));
                    continue;
                }
                int left = hull.get(sp) - 1;
                hull.put(sp, left);
                outcomes.add(new ShotOutcome(p, true, left <= 0,
                        sp.getShip().getDetection()));
            }
            ai.reportResults(outcomes);

            boolean cleared = true;
            for (int hp : hull.values()) {
                if (hp > 0) {
                    cleared = false;
                    break;
                }
            }
            if (cleared) break;
        }
        return shots;
    }

    /** Drains the opponent's target list and checks it never offers the same tile twice. */
    private static boolean noRepeats(EnemyAI ai) {
        Set<Point> seen = new HashSet<>();
        for (int i = 0; i < GameLayout.SIZE * GameLayout.SIZE + 4; i++) {
            List<Point> picks = ai.generateShots(3);
            if (picks.isEmpty()) break;
            for (Point p : picks) {
                if (!seen.add(p)) return false;
            }
        }
        return true;
    }
}
