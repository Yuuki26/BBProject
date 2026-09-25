package Test;

import Ships.Submarine;
import Ships.vessels.Balao;
import Ships.vessels.Baltimore;
import Ships.vessels.Essex;
import Ships.vessels.Montana;
import Ships.vessels.Nagato;
import Ships.PlayerFleet;
import Ships.FleetCalculation;
import Ships.ShipStats;
import Ships.Ship_Placement;
import Ships.Ships_Type;
import Ships.StealthMap;
import Ships.vessels.I_556;
import Ships.vessels.Helena;
import Ships.vessels.VesselRegistry;
import com.bb.EliteEnemy;
import com.bb.EnemyAI;
import com.bb.Frames;
import com.bb.GameLayout;
import com.bb.OpponentGenerator;
import com.bb.NormalEnenmy;
import com.bb.OpponentPanel;
import com.bb.Reward;
import com.bb.RewardPanel;
import com.bb.RunState;
import com.bb.SaveManager;
import com.bb.ShipStatsCard;
import com.bb.ShotOutcome;
import com.bb.Shop;
import com.bb.ShopPanel;
import com.bb.StarterFleetPanel;
import skills.Attacker_Skills_Instance;
import skills.Defender_Skills_Instance;
import skills.ERA_BRONZE;
import skills.ERA_GOLD;
import skills.ERA_silver;
import skills.Enhance;
import skills.ModifiedStats;
import skills.Rapid_Fire;
import skills.Rarity;
import skills.SkillPool;
import skills.Skills;
import skills.SkillsRegistry;
import skills.Skills_Register;
import skills.modify_DMG;
import skills.modify_HP;
import skills.modify_shield;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
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

    /** Counts a failure without printing a line, for checks inside a loop. */
    private static void checkQuiet(boolean ok) {
        if (!ok) {
            System.out.println("  FAIL  (inside a loop)");
            failures++;
        }
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
            freshBoard(player);
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
            freshBoard(player);

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
                freshBoard(player);
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
            freshBoard(player);
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
            PlayerFleet playerFleet = new PlayerFleet();
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
            freshBoard(player);
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
            check("the battle starts before saving", player.startBattle());

            RunState.current().setCurrency(321);
            RunState.current().setBudgetBonus(4);

            // A damaged ship waiting in port: its damage must survive the save too.
            Ship_Placement inPort = null;
            for (Ship_Placement sp : player.getFleet().getPlacements()) {
                if (!player.isDeployed(sp)) {
                    inPort = sp;
                    break;
                }
            }
            String inPortClass = inPort == null ? "" : inPort.getShip().getClass().getName();
            int inPortMax = inPort == null ? 0 : inPort.getShip().getHP();
            if (inPort != null) player.restoreHull(inPort, inPortMax / 3, inPortMax);

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
            eq("gold restored", RunState.current().getCurrency(), 321);
            eq("bought fleet cost restored", RunState.current().getBudgetBonus(), 4);
            eq("deployed ships restored", player.getDeployedPlacements().size(), shipsBefore);

            Ship_Placement portAfter = null;
            for (Ship_Placement sp : player.getFleet().getPlacements()) {
                if (!player.isDeployed(sp) && sp.getShip().getClass().getName().equals(inPortClass)) {
                    portAfter = sp;
                }
            }
            check("a damaged ship in port is still damaged after loading",
                    portAfter != null && player.currentHP(portAfter) == inPortMax / 3);
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
            check("a save made mid-battle loads mid-battle", player.isBattleStarted());

            // Saves from before the Start button carry no flag. Shots already fired mean that
            // battle had started, so it has to come back locked in, not reopened for deploying.
            try {
                java.nio.file.Path slot = SaveManager.slotPath(9);
                List<String> lines = new ArrayList<>(java.nio.file.Files.readAllLines(slot));
                lines.removeIf(line -> line.startsWith("battleStarted="));
                java.nio.file.Files.write(slot, lines);

                player.resetBoard();
                String legacyError = SaveManager.load(9, player, opponent);
                check("a save without the flag still loads (" + legacyError + ")",
                        legacyError == null);
                check("and comes back mid-battle, since shots had been fired",
                        player.isBattleStarted());
            } catch (java.io.IOException e) {
                check("rewrote the save without its flag (" + e.getMessage() + ")", false);
            }

            // A save made while deploying comes back still deploying.
            freshBoard(player);
            player.autoDeploy();
            SaveManager.save(9, player, opponent);
            player.resetBoard();
            SaveManager.load(9, player, opponent);
            check("a save made before Start loads before Start", !player.isBattleStarted());

            try {
                java.nio.file.Files.deleteIfExists(SaveManager.slotPath(9));
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        });

        SwingUtilities.invokeAndWait(() -> {
            header("10. Rewards apply to the run");
            RunState.startNewRun();
            freshBoard(player);
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

            // Ships only come from the shop now, so no reward can hand one out.
            boolean shipKind = false;
            for (Reward.Kind k : Reward.Kind.values()) shipKind |= k.name().equals("SHIP");
            check("there is no ship reward any more", !shipKind);
            int fleetBefore = player.getFleet().getPlacements().size();
            Random rolls = new Random(5);
            for (int i = 0; i < 300; i++) {
                for (Reward r : Reward.roll(player, 3, rolls)) r.apply(player);
            }
            eq("300 reward screens later the fleet is the same size",
                    player.getFleet().getPlacements().size(), fleetBefore);

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
            freshBoard(player);

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
            freshBoard(player);
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
            freshBoard(player);

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

            // Two smaller ships that fit together fit just as well. Worked out from their
            // current costs, so rebalancing a ship does not break the test.
            freshBoard(player);
            Ship_Placement cb = findInRoster(player, "CB");
            Ship_Placement cl = findInRoster(player, "CL");
            if (cb != null && cl != null) {
                int pair = cb.getShip().getCost() + cl.getShip().getCost();
                cb.setOrigin(new Point(0, 0));
                player.commitPlacement(cb);
                cl.setOrigin(new Point(0, 6));
                check("a heavy and a light cruiser (" + pair + ") fit together exactly when "
                        + "they fit the budget", player.validatePlacement(cl) == (pair <= budget));
                if (pair <= budget) {
                    player.commitPlacement(cl);
                    eq("and spend what they add up to", player.getDeployedCost(), pair);
                }
            }

            // One ship on the board is enough to fight; a full roster is not required.
            check("one deployed ship is enough to start", player.isReadyForBattle());

            // The reported bug: a refused placement used to leave the ship's origin pointing
            // at the square it was refused from, and "deployed" was decided by that origin,
            // so the ship kept costing budget it had never been allowed to spend.
            freshBoard(player);
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
            check("a refused drop leaves the board ready to start",
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
            freshBoard(player);
            player.autoDeploy();
            check("autoDeploy stays inside the budget",
                    player.getDeployedCost() <= RunState.current().getDeploymentBudget());
            check("autoDeploy actually spends the budget", player.getDeployedCost() > 0);

            header("15b. Hull classes and cost");
            String[] hulls = {"CV", "BB", "CB", "CL", "DD", "SS"};
            for (String hull : hulls) {
                check("at least two vessels for " + hull,
                        Ships.vessels.VesselRegistry.byHull(hull).size() >= 2);
            }

            // Cost belongs to each vessel, so two ships of one class can be priced apart.
            eq("Nagato's cost", new Nagato().getCost(), 10);
            check("ships of the same class can cost different amounts",
                    new Ships.vessels.Monarch().getCost() < new Nagato().getCost());
            check("every registered vessel costs at least 1",
                    Ships.vessels.VesselRegistry.cheapestCost() >= 1);
            eq("a destroyer is still the cheapest thing afloat",
                    Ships.vessels.VesselRegistry.cheapestCost(), new I_556().getCost());
            check("a battleship costs more than a destroyer",
                    new Nagato().getCost() > new I_556().getCost());

            // Fixed once built: final, so no subclass can override it...
            try {
                java.lang.reflect.Method m = Ships.vessels.Monarch.class.getMethod("getCost");
                check("getCost is final, so no vessel can change it",
                        java.lang.reflect.Modifier.isFinal(m.getModifiers()));
            } catch (NoSuchMethodException e) {
                check("getCost exists", false);
            }

            // ...and copied out of the stat block, so editing the stats afterwards does not
            // reprice the ship.
            ShipStats shared = new ShipStats().hp(5).size(2).cost(4);
            Ships_Type priced = new Ships.Destroyer("Test", shared) {};
            shared.cost(99);
            eq("changing the stats after building does not change the cost",
                    priced.getCost(), 4);

            // A vessel that forgets its cost is refused, rather than sailing for free.
            boolean refused = false;
            try {
                new Ships.Destroyer("Forgot", new ShipStats().hp(5).size(2)) {};
            } catch (IllegalStateException e) {
                refused = true;
            }
            check("a vessel with no cost cannot be built", refused);

            // Enemy fleets are budgeted in cost, so a later stage must be able to spend more.
            OpponentGenerator gen = new OpponentGenerator(GameLayout.SIZE);
            PlayerFleet pf = new PlayerFleet();
            eq("a new player fleet starts empty, waiting for the starter pick",
                    pf.getPlacements().size(), 0);

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

        SwingUtilities.invokeAndWait(() -> {
            header("17. Start checks the fleet, locks it in and opens the enemy board");
            RunState.startNewRun();
            freshBoard(player);
            showCard(frames, "PLAYER");

            List<JButton> starts = buttonsNamed(cards(frames), "Start");
            List<JButton> enemyBoards = buttonsNamed(cards(frames), "Enemy Board (Q)");
            eq("one Start button under each battle screen", starts.size(), 2);
            check("Start is showing while deploying", allVisible(starts, true));
            check("Enemy Board is hidden until Start", allVisible(enemyBoards, false));
            check("the battle has not started", !player.isBattleStarted());

            eq("Q is still bound to the enemy board",
                    cards(frames).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                            .get(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0)),
                    "showOpponent");
            pressKey(frames, "showOpponent");
            check("but Q does not reach it before Start",
                    cardShowing(frames, player) && !cardShowing(frames, opponent));

            JButton start = starts.get(0);
            start.doClick();
            check("Start is refused with nothing deployed", !player.isBattleStarted());
            check("and you stay on your board", cardShowing(frames, player));

            // The cost check. Placement never lets the budget be overspent, but a fleet
            // restored from a save skips placement entirely - so build an over-budget board
            // that way, and make sure Start still refuses to sail it.
            Ship_Placement bb = findInRoster(player, "BB");
            Ship_Placement cv = findInRoster(player, "CV");
            bb.setOrigin(new Point(0, 0));
            player.restoreShip(bb, bb.getShip().getHP(), bb.getShip().getHP());
            cv.setOrigin(new Point(0, 4));
            player.restoreShip(cv, cv.getShip().getHP(), cv.getShip().getHP());
            player.refreshAfterLoad();

            int budget = RunState.current().getDeploymentBudget();
            check("the board is over budget (cost " + player.getDeployedCost()
                    + " / " + budget + ")", player.getDeployedCost() > budget);
            start.doClick();
            check("Start refuses an over-budget fleet", !player.isBattleStarted());
            check("and you stay on your board", cardShowing(frames, player));

            // Inside the budget it goes through. One destroyer only, so there is budget left
            // over - otherwise the lock checks below would pass for want of money alone.
            freshBoard(player);
            Ship_Placement dd = findInRoster(player, "DD");
            player.beginCarryAt(dd, new Point(0, 0));
            player.endCarryAt(new Point(0, 0));
            check("a destroyer is deployed with budget to spare",
                    player.isDeployed(dd) && player.getRemainingBudget() > 0
                            && !player.isBudgetExhausted());
            start.doClick();
            check("Start begins the battle", player.isBattleStarted());
            check("and takes you to the enemy board", cardShowing(frames, opponent));
            check("Enemy Board has taken Start's place",
                    allVisible(enemyBoards, true) && allVisible(starts, false));

            // Locked in: nothing moves once the battle is on.
            Ship_Placement ship = player.getDeployedPlacements().get(0);
            Point before = new Point(ship.getOrigin());
            player.beginCarryAt(ship, new Point(7, 7));
            check("a deployed ship cannot be picked up mid-battle", !player.isCarrying());
            check("so it stays exactly where it was",
                    player.isDeployed(ship) && ship.getOrigin().equals(before));
            int deployed = player.deployedCount();
            player.autoDeploy();
            eq("auto-deploy does nothing mid-battle, budget or not",
                    player.deployedCount(), deployed);

            Ship_Placement spare = findInRoster(player, "SS");
            check("a submarine is still in port, and affordable",
                    spare != null && player.canAfford(spare));
            if (spare != null) {
                player.beginCarryAt(spare, new Point(5, 5));
                player.endCarryAt(new Point(5, 5));
                check("but it cannot be brought out mid-battle", !player.isDeployed(spare));
            }

            showCard(frames, "PLAYER");
            pressKey(frames, "showOpponent");
            check("Q reaches the enemy board once the battle is on",
                    cardShowing(frames, opponent));

            // Clearing the stage opens a fresh deployment phase for the next one.
            frames.triggerStageWon(10);
            List<JButton> takes = buttonsNamed(cards(frames), "Take");
            check("the reward screen has something to take", !takes.isEmpty());
            if (!takes.isEmpty()) takes.get(0).doClick();
            check("the next stage opens in deployment", !player.isBattleStarted());
            check("with Start back in place",
                    allVisible(starts, true) && allVisible(enemyBoards, false));
            check("and you on your board to deploy", cardShowing(frames, player));
        });

        SwingUtilities.invokeAndWait(() -> {
            header("18. Skill rarity, weight and upgrades");

            int openers = 0;
            for (Skills s : Skills_Register.getAllSkills()) {
                if (s.rarity().isUnlockedAt(1)) openers++;
            }
            check("at least two skills are open to start a run with (" + openers + ")",
                    openers >= 2);

            Rarity[] tiers = Rarity.values();
            boolean ordered = true;
            for (int i = 1; i < tiers.length; i++) {
                ordered &= tiers[i].getBaseWeight() < tiers[i - 1].getBaseWeight();
                ordered &= tiers[i].getUnlockStage() >= tiers[i - 1].getUnlockStage();
            }
            check("each rarer tier weighs less and unlocks no earlier", ordered);

            // There is no opening skill pick any more: a run starts bare and earns its skills.
            List<Skills> leftover = new ArrayList<>();
            leftover.add(new modify_DMG());
            player.setActiveSkills(leftover);
            frames.startNewRun();
            check("a new run goes straight to the starter fleet",
                    cardShowing(frames, frames.getStarterFleetPanel()));
            eq("with no skills from the last run", RunState.current().getLoadout().size(), 0);
            eq("and none active", SkillsRegistry.getSelectedSkills().size(), 0);

            // Weights, one rule at a time.
            List<Skills> none = new ArrayList<>();
            int goldUnlocks = new ERA_GOLD().rarity().getUnlockStage();
            eq("a skill has no weight before its rarity unlocks",
                    SkillPool.weightOf(new ERA_GOLD(), goldUnlocks - 1, none), 0);
            check("and some once the run gets there",
                    SkillPool.weightOf(new ERA_GOLD(), goldUnlocks, none) > 0);

            List<Skills> bronze = new ArrayList<>();
            bronze.add(new ERA_BRONZE());
            eq("owning Bronze multiplies Silver's weight",
                    SkillPool.weightOf(new ERA_silver(), 2, bronze),
                    SkillPool.weightOf(new ERA_silver(), 2, none)
                            * SkillPool.UPGRADE_WEIGHT_MULTIPLIER);
            check("Gold is an upgrade of Bronze, by way of Silver",
                    SkillPool.isUpgradeOf(new ERA_GOLD(), new ERA_BRONZE()));
            eq("a skill already owned is not offered again",
                    SkillPool.weightOf(new ERA_BRONZE(), 2, bronze), 0);

            List<Skills> gold = new ArrayList<>();
            gold.add(new ERA_GOLD());
            eq("nor is a downgrade of one", SkillPool.weightOf(new ERA_silver(), 6, gold), 0);

            // The roll itself, over many reward screens.
            RunState.startNewRun();
            freshBoard(player);
            player.autoDeploy();
            List<Skills> withBronze = new ArrayList<>();
            withBronze.add(new ERA_BRONZE());
            withBronze.add(new modify_DMG());
            List<Skills> withoutBronze = new ArrayList<>();
            withoutBronze.add(new Rapid_Fire());
            withoutBronze.add(new modify_DMG());

            int screens = 3000;
            int[] silverSeen = new int[2];
            boolean gateHeld = true;
            boolean ownedOffered = false;
            for (int variant = 0; variant < 2; variant++) {
                player.setActiveSkills(variant == 0 ? withBronze : withoutBronze);
                RunState.current().setStage(2);
                Random rand = new Random(99);
                for (int i = 0; i < screens; i++) {
                    for (Reward r : Reward.roll(player, 3, rand)) {
                        if (r.getSkill() == null) continue;
                        if (r.getRarity().getUnlockStage() > 2) gateHeld = false;
                        if (RunState.current().hasSkill(r.getSkill().getClass())) ownedOffered = true;
                        if (r.getSkill() instanceof ERA_silver) silverSeen[variant]++;
                    }
                }
            }
            System.out.printf("        Silver offered on %.0f%% of screens holding Bronze, %.0f%% without%n",
                    100.0 * silverSeen[0] / screens, 100.0 * silverSeen[1] / screens);
            check("no screen offers a skill its stage has not unlocked", gateHeld);
            check("no screen offers a skill already owned", !ownedOffered);
            check("owning Bronze makes Silver far likelier to come up",
                    silverSeen[0] > silverSeen[1] * 1.5);

            // Taking the upgrade swaps it in rather than stacking it.
            player.setActiveSkills(withBronze);
            RunState.current().setStage(2);
            Reward silverCard = Reward.skill(new ERA_silver());
            check("the Silver card knows it is an upgrade", silverCard.isUpgrade());
            silverCard.apply(player);
            check("taking Silver removes Bronze", !RunState.current().hasSkill(ERA_BRONZE.class));
            check("and adds Silver", RunState.current().hasSkill(ERA_silver.class));
            eq("so the loadout stays the same size", RunState.current().getLoadout().size(), 2);

            boolean registryHasBronze = false;
            boolean registryHasSilver = false;
            for (Skills s : SkillsRegistry.getSelectedSkills()) {
                registryHasBronze |= s instanceof ERA_BRONZE;
                registryHasSilver |= s instanceof ERA_silver;
            }
            check("and the active skills see the swap too", registryHasSilver && !registryHasBronze);
        });

        // A known enemy for section 19: Baltimore on A1-C1, I-556 on A4-B4, I-141 on F7-G7.
        final Ship_Placement[] enemy = new Ship_Placement[3];
        final int[] hit = new int[2];   // full and half damage per shot this fight

        SwingUtilities.invokeAndWait(() -> {
            header("19. Crippled ships can be fired on again, at half damage");
            RunState.startNewRun();
            freshBoard(player);
            player.setActiveSkills(new ArrayList<>());   // no damage modifiers in play

            Ship_Placement nagato = findInRoster(player, "BB");
            player.beginCarryAt(nagato, new Point(0, 7));
            player.endCarryAt(new Point(0, 7));
            check("the player sails one battleship", player.deployedCount() == 1);
            player.startBattle();

            enemy[0] = new Ship_Placement(new Baltimore(), new Point(0, 0), true);
            enemy[1] = new Ship_Placement(new I_556(), new Point(0, 3), true);
            enemy[2] = new Ship_Placement(new Ships.vessels.I_141(), new Point(5, 6), true);
            List<Ship_Placement> enemyFleet = new ArrayList<>(Arrays.asList(enemy));
            List<Integer> hulls = new ArrayList<>();
            for (Ship_Placement sp : enemyFleet) hulls.add(sp.getShip().getHP());
            opponent.restoreStage(enemyFleet, hulls, new boolean[GameLayout.SIZE][GameLayout.SIZE]);
            showCard(frames, "OPPONENT");

            float perHit = FleetCalculation.damageBetween(player.getAlivePlacements(), enemyFleet,
                    new ModifiedStats().dmgModifier(), 1f);
            hit[0] = Math.max(1, Math.round(perHit));
            hit[1] = Math.max(1, Math.round(perHit / 2f));
            System.out.println("        full hit " + hit[0] + ", half hit " + hit[1]);

            fire(opponent);
            target(opponent, "A1");
            target(opponent, "B1");
            target(opponent, "A4");
            eq("a fresh target is previewed at full damage",
                    cellAt(opponent, "A1").getClientProperty("target"), "full");
            check("with a crosshair, not a grey fill",
                    cellAt(opponent, "A1").getIcon() != null
                            && cellAt(opponent, "A1").getBackground().equals(Color.WHITE));
            confirm(opponent);
            check("the preview clears once the salvo is fired",
                    cellAt(opponent, "A1").getClientProperty("target") == null
                            && cellAt(opponent, "A1").getIcon() == null);
        });
        Thread.sleep(1400);   // the enemy's reply lands 800 ms after a salvo

        SwingUtilities.invokeAndWait(() -> {
            fire(opponent);
            target(opponent, "A1");
            check("a hit tile on a ship still above half, with sections unfound, is refused",
                    cellAt(opponent, "A1").getClientProperty("target") == null);
            target(opponent, "B4");   // I-556 drops below half
            target(opponent, "C1");   // Baltimore's last hidden section: now fully located
            target(opponent, "H8");   // open water
            confirm(opponent);
            eq("I-556 is crippled", opponent.getOpponentShipHP(enemy[1]),
                    enemy[1].getShip().getHP() - 2 * hit[0]);
        });
        Thread.sleep(1400);

        SwingUtilities.invokeAndWait(() -> {
            fire(opponent);
            JButton a4 = cellAt(opponent, "A4");
            Color yellowBefore = a4.getBackground();
            target(opponent, "A4");   // crippled (yellow): re-fire at half
            eq("a crippled ship's hit tile can be targeted again, previewed at half damage",
                    a4.getClientProperty("target"), "half");
            check("and its yellow stays visible under the crosshair",
                    a4.getBackground().equals(yellowBefore) && a4.getIcon() != null);
            target(opponent, "A1");   // Baltimore: every tile hit, still above half
            eq("a ship with every tile already hit can be re-fired too, so it can still be sunk",
                    cellAt(opponent, "A1").getClientProperty("target"), "half");
            target(opponent, "B1");
            confirm(opponent);

            eq("the re-fire finished off I-556", opponent.getOpponentShipHP(enemy[1]), 0);
            eq("Baltimore took three full hits and two half ones",
                    opponent.getOpponentShipHP(enemy[0]),
                    enemy[0].getShip().getHP() - 3 * hit[0] - 2 * hit[1]);
            check("the untouched I-141 keeps the stage going",
                    opponent.getOpponentShipHP(enemy[2]) == enemy[2].getShip().getHP());
        });
        Thread.sleep(1400);

        SwingUtilities.invokeAndWait(() -> {
            header("20. Starter fleet: three ships, one reroll each");
            frames.startNewRun();
            StarterFleetPanel starter = frames.getStarterFleetPanel();
            check("a new run opens on the starter fleet", cardShowing(frames, starter));
            eq("with an empty roster until it is confirmed", player.getFleet().getPlacements().size(), 0);

            // Worked out from the ships' current costs, so rebalancing does not break it.
            int budget = RunState.current().getDeploymentBudget();
            List<VesselRegistry.Entry> fits = new ArrayList<>();
            List<VesselRegistry.Entry> dear = new ArrayList<>();
            for (VesselRegistry.Entry e : VesselRegistry.all()) {
                (e.getCost() <= budget ? fits : dear).add(e);
            }

            int deals = 400;
            boolean threeEach = true;
            boolean eachFits = true;
            boolean allDifferent = true;
            int overBudget = 0;
            Set<String> dealt = new HashSet<>();
            boolean hullKept = true;
            boolean neverRepeats = true;
            boolean refusedOnlyWhenEmpty = true;
            boolean onceOnly = true;
            int rerolled = 0;
            int nothingLeft = 0;
            for (int d = 0; d < deals; d++) {
                starter.reset(new Random(d));
                List<VesselRegistry.Entry> offers = starter.getOffers();
                threeEach &= offers.size() == Math.min(3, fits.size());
                Set<String> shown = new HashSet<>();
                int total = 0;
                for (VesselRegistry.Entry e : offers) {
                    eachFits &= e.getCost() <= budget;
                    allDifferent &= shown.add(e.getName());
                    dealt.add(e.getName());
                    total += e.getCost();
                }
                if (total > budget) overBudget++;

                // Reroll every slot twice: the first may go through, the second never does.
                for (int i = 0; i < offers.size(); i++) {
                    VesselRegistry.Entry before = starter.getOffers().get(i);
                    List<String> expected = new ArrayList<>();
                    for (VesselRegistry.Entry e : VesselRegistry.all()) {
                        if (e.getHullCode().equals(before.getHullCode()) && e.getCost() <= budget
                                && !shown.contains(e.getName())) {
                            expected.add(e.getName());
                        }
                    }
                    String refusal = starter.reroll(i);
                    VesselRegistry.Entry after = starter.getOffers().get(i);
                    if (expected.isEmpty()) {
                        nothingLeft++;
                        refusedOnlyWhenEmpty &= refusal != null && after == before;
                    } else {
                        rerolled++;
                        refusedOnlyWhenEmpty &= refusal == null;
                        hullKept &= after.getHullCode().equals(before.getHullCode())
                                && after.getCost() <= budget;
                        neverRepeats &= expected.contains(after.getName()) && shown.add(after.getName());
                    }
                    VesselRegistry.Entry settled = starter.getOffers().get(i);
                    onceOnly &= starter.reroll(i) != null && starter.getOffers().get(i) == settled;
                }
            }
            System.out.println("        " + deals + " deals: " + overBudget + " cost more than "
                    + budget + " in all; " + rerolled + " rerolls went through, " + nothingLeft
                    + " had nothing left to roll");
            check("every deal is three ships", threeEach);
            check("each costing no more than the fleet cost of " + budget, eachFits);
            check("and all three different", allDifferent);
            check("together they may cost more than the fleet cost", overBudget > 0);
            eq("every ship that fits turns up in some deal", dealt.size(), fits.size());
            boolean noDear = true;
            for (VesselRegistry.Entry e : dear) noDear &= !dealt.contains(e.getName());
            check("and none of the " + dear.size() + " that don't fit ever does", noDear);
            check("a reroll keeps the hull class and the cost rule", rerolled > 0 && hullKept);
            check("and never lands on a ship shown before - on the table or rerolled away",
                    neverRepeats);
            check("it is refused, not wasted, only when that class has nothing left",
                    nothingLeft > 0 && refusedOnlyWhenEmpty);
            check("each ship rerolls once", onceOnly);

            // The buttons: one per ship, spent after one use. Dealt until all three can reroll.
            int seed = 0;
            boolean allCan;
            do {
                starter.reset(new Random(seed++));
                allCan = true;
                for (int i = 0; i < starter.getOffers().size(); i++) {
                    allCan &= !starter.rerollChoices(i).isEmpty();
                }
            } while (!allCan && seed < 500);
            eq("each card has its own reroll button", buttonsNamed(starter, "Reroll (1 left)").size(),
                    starter.getOffers().size());
            buttonsNamed(starter, "Reroll (1 left)").get(0).doClick();
            List<JButton> spent = buttonsNamed(starter, "Rerolled");
            check("clicking one spends that one only", spent.size() == 1 && !spent.get(0).isEnabled()
                    && starter.getRerollsLeft(0) == 0 && starter.getRerollsLeft(1) == 1);

            // A ship whose class has nothing left says so on the button.
            seed = 0;
            int stuck = -1;
            do {
                starter.reset(new Random(seed++));
                for (int i = 0; i < starter.getOffers().size() && stuck < 0; i++) {
                    if (starter.rerollChoices(i).isEmpty()) stuck = i;
                }
            } while (stuck < 0 && seed < 500);
            String stuckClass = starter.getOffers().get(stuck).create().getHullClass().toLowerCase();
            List<JButton> none = buttonsNamed(starter, "No other " + stuckClass);
            check("a ship with nothing left to roll into says \"No other " + stuckClass
                    + "\" instead", !none.isEmpty() && !none.get(0).isEnabled());

            // Setting sail installs exactly the three on the table, even over budget.
            seed = 0;
            int total;
            do {
                starter.reset(new Random(seed++));
                total = 0;
                for (VesselRegistry.Entry e : starter.getOffers()) total += e.getCost();
            } while (total <= budget && seed < 500);
            List<String> onTable = new ArrayList<>();
            for (VesselRegistry.Entry e : starter.getOffers()) onTable.add(e.getName());
            buttonsNamed(starter, "Set sail").get(0).doClick();
            check("setting sail goes to your board", cardShowing(frames, player));
            List<String> inRoster = new ArrayList<>();
            for (Ship_Placement sp : player.getFleet().getPlacements()) inRoster.add(sp.getShip().getName());
            eq("the roster is exactly the three dealt", inRoster, onTable);
            eq("nothing is deployed yet", player.deployedCount(), 0);
            check("a fleet costing " + total + " is accepted with a fleet cost of " + budget,
                    player.getFleet().totalCost() == total && total > budget);
            player.autoDeploy();
            check("and it sails with what fits", player.startBattle()
                    && player.getDeployedCost() <= budget && player.deployedCount() < 3);

            // The rule outside the screen: each ship within budget, not the total.
            if (!dear.isEmpty()) {
                List<Ships_Type> tooBig = new ArrayList<>();
                tooBig.add(dear.get(0).create());
                check("a ship dearer than the whole fleet cost is refused ("
                                + dear.get(0).getName() + ")",
                        player.setStarterFleet(tooBig) != null && player.getFleet().size() == 3);
            }
            check("and an empty fleet is refused", player.setStarterFleet(new ArrayList<>()) != null);
        });

        SwingUtilities.invokeAndWait(() -> {
            header("21. Shop");
            int[] opens = {4, 7, 10, 13};
            int[] shut = {1, 2, 3, 5, 6, 8, 9};
            boolean schedule = true;
            for (int s : opens) schedule &= Shop.opensBefore(s);
            for (int s : shut) schedule &= !Shop.opensBefore(s);
            check("the shop opens before stages 4, 7, 10, 13 and no others", schedule);

            // Clearing stage 3 pays out, and the shop opens after the reward.
            RunState.startNewRun();
            freshBoard(player);
            player.autoDeploy();
            player.startBattle();
            RunState.current().setStage(3);
            int goldBefore = RunState.current().getCurrency();
            frames.triggerStageWon(10);
            eq("clearing stage 3 pays its gold",
                    RunState.current().getCurrency() - goldBefore, RunState.currencyForStage(3));
            buttonsNamed(cards(frames), "Take").get(0).doClick();
            ShopPanel shopScreen = frames.getShopPanel();
            check("the shop opens before stage 4", cardShowing(frames, shopScreen));
            Shop shop = shopScreen.getShop();
            check("it has ships for sale", !shop.getShipsForSale().isEmpty());

            // Buying a ship.
            RunState.current().setCurrency(1000);
            int rosterBefore = player.getFleet().size();
            Ships_Type offered = shop.getShipsForSale().get(0);
            String bought = shop.buyShip(0, player);
            check("buying a ship goes through (" + bought + ")", bought == null);
            eq("it joins the roster", player.getFleet().size(), rosterBefore + 1);
            eq("and costs its price", RunState.current().getCurrency(), 1000 - Shop.priceOf(offered));
            check("each offer sells only once", shop.buyShip(0, player) != null);
            if (shop.getShipsForSale().size() > 1) {
                RunState.current().setCurrency(0);
                check("no gold, no ship", shop.buyShip(1, player) != null
                        && player.getFleet().size() == rosterBefore + 1);
            }

            // Expanding the fleet.
            RunState.current().setCurrency(1000);
            int budgetBefore = RunState.current().getDeploymentBudget();
            Shop.Expansion ex = shop.getExpansion();
            check("buying the expansion goes through", shop.buyExpansion() == null);
            eq("it raises the fleet cost", RunState.current().getDeploymentBudget(),
                    budgetBefore + ex.getAmount());
            check("once per visit", shop.buyExpansion() != null);
            eq("the enemy's budget ignores what the player bought",
                    RunState.deploymentBudgetForStage(4),
                    RunState.STARTING_DEPLOYMENT_BUDGET + 3 * RunState.BUDGET_GAIN_PER_STAGE);

            // A bought expansion must not grow the enemy with it.
            RunState.current().setBudgetBonus(40);
            OpponentGenerator enemyGen = new OpponentGenerator(GameLayout.SIZE);
            int worstEnemy = 0;
            for (int i = 0; i < 20; i++) {
                int spent = 0;
                for (Ship_Placement sp : enemyGen.buildOpponentFleet(player.getFleet(), 1)) {
                    spent += sp.getShip().getCost();
                }
                worstEnemy = Math.max(worstEnemy, spent);
            }
            check("with 40 fleet cost bought, a stage 1 enemy still costs at most "
                    + RunState.deploymentBudgetForStage(1) + " (" + worstEnemy + ")",
                    worstEnemy <= RunState.deploymentBudgetForStage(1));
            RunState.current().setBudgetBonus(ex.getAmount());

            // Checked against the numbers asked for, not the enum's own, so retuning the
            // enum by mistake shows up here.
            int[] wanted = {50, 30, 15, 5};
            int[] seen = new int[Shop.Expansion.values().length];
            Random dice = new Random(11);
            int rolls = 20000;
            for (int i = 0; i < rolls; i++) seen[Shop.rollExpansion(dice).ordinal()]++;
            boolean oddsRight = true;
            StringBuilder odds = new StringBuilder();
            for (Shop.Expansion e : Shop.Expansion.values()) {
                double pct = 100.0 * seen[e.ordinal()] / rolls;
                odds.append(String.format("%s +%d %.1f%%  ", e.getRarity().getLabel(),
                        e.getAmount(), pct));
                oddsRight &= Math.abs(pct - wanted[e.ordinal()]) < 1.5;
            }
            System.out.println("        " + odds);
            check("expansions come up 50 / 30 / 15 / 5", oddsRight);
            check("and are sized 2 / 4 / 6 / 10",
                    Shop.Expansion.UNCOMMON.getAmount() == 2 && Shop.Expansion.RARE.getAmount() == 4
                            && Shop.Expansion.EPIC.getAmount() == 6
                            && Shop.Expansion.LEGENDARY.getAmount() == 10);

            // Selling: 60% back, deployed or not.
            Ship_Placement deployed = player.getDeployedPlacements().get(0);
            int gold = RunState.current().getCurrency();
            int costBefore = player.getDeployedCost();
            check("selling a deployed ship goes through", Shop.sell(player, deployed) == null);
            eq("it pays back 60% of its price", RunState.current().getCurrency(),
                    gold + Shop.priceOf(deployed.getShip()) * 60 / 100);
            check("and it leaves the board and the fleet",
                    !player.isDeployed(deployed)
                            && !player.getFleet().getPlacements().contains(deployed)
                            && player.getDeployedCost() < costBefore);
            while (player.getFleet().size() > 1) {
                Shop.sell(player, player.getFleet().getPlacements().get(0));
            }
            check("the last ship cannot be sold",
                    Shop.sell(player, player.getFleet().getPlacements().get(0)) != null
                            && player.getFleet().size() == 1);

            // Repairs.
            freshBoard(player);
            player.autoDeploy();
            Ship_Placement a = player.getDeployedPlacements().get(0);
            Ship_Placement b = null;
            for (Ship_Placement sp : player.getFleet().getPlacements()) {
                if (!player.isDeployed(sp)) b = sp;   // one in port, so both kinds are covered
            }
            int aMax = player.getShipMaxHP(a);
            int bMax = player.getShipMaxHP(b);
            player.restoreHull(a, aMax / 2, aMax);
            player.restoreHull(b, bMax / 4, bMax);
            RunState.current().setCurrency(0);
            check("no gold, no repair",
                    Shop.repair(player, a) != null && player.currentHP(a) == aMax / 2);

            RunState.current().setCurrency(1000);
            int onePrice = Shop.repairPrice(player, a);
            check("a repair has a price", onePrice > 0);
            check("repairing one ship goes through", Shop.repair(player, a) == null);
            check("it is back to full hull, and paid for",
                    player.currentHP(a) == aMax
                            && RunState.current().getCurrency() == 1000 - onePrice);

            player.restoreHull(a, aMax / 3, aMax);
            int allPrice = Shop.repairAllPrice(player);
            eq("repair-all costs the sum of the separate repairs", allPrice,
                    Shop.repairPrice(player, a) + Shop.repairPrice(player, b));
            gold = RunState.current().getCurrency();
            check("repairing everything goes through", Shop.repairAll(player) == null);
            check("both ships, one of them in port, are back to full",
                    player.currentHP(a) == aMax && player.currentHP(b) == bMax
                            && RunState.current().getCurrency() == gold - allPrice);

            // Leaving the shop starts the next stage on the board.
            buttonsNamed(shopScreen, "Leave shop").get(0).doClick();
            check("leaving the shop goes to your board for stage 4",
                    cardShowing(frames, player) && RunState.current().getStage() == 4
                            && !player.isBattleStarted());

            header("22. Damage stays with a ship in port");
            freshBoard(player);
            Ship_Placement dd = findInRoster(player, "DD");
            player.beginCarryAt(dd, new Point(0, 0));
            player.endCarryAt(new Point(0, 0));
            int ddMax = player.getShipMaxHP(dd);
            player.restoreHull(dd, ddMax / 2, ddMax);
            player.beginCarryAt(dd, null);
            player.endCarryAt(null);
            check("a damaged ship sent to port keeps its damage",
                    !player.isDeployed(dd) && player.currentHP(dd) == ddMax / 2);
            player.beginCarryAt(dd, new Point(0, 0));
            player.endCarryAt(new Point(0, 0));
            check("and brings it back out - port is not a free repair",
                    player.isDeployed(dd) && player.currentHP(dd) == ddMax / 2);

            // Ships in port now keep hull records, which must not count towards staying afloat.
            Ship_Placement spare = findInRoster(player, "CL");
            player.restoreHull(spare, player.getShipMaxHP(spare), player.getShipMaxHP(spare));
            player.restoreHull(dd, 0, ddMax);
            check("with everything on the board sunk, the fleet is beaten, whatever is in port",
                    player.isDefeated());
        });

        SwingUtilities.invokeAndWait(() -> {
            header("23. The enemy re-fires on crippled ships too");
            RunState.startNewRun();
            RunState.current().setStage(3);   // room in the budget for both ships below
            freshBoard(player);
            player.setActiveSkills(new ArrayList<>());
            Ship_Placement dd = findInRoster(player, "DD");
            Ship_Placement bb = findInRoster(player, "BB");
            player.beginCarryAt(dd, new Point(0, 0));   // A1-B1
            player.endCarryAt(new Point(0, 0));
            player.beginCarryAt(bb, new Point(0, 7));   // A8-D8
            player.endCarryAt(new Point(0, 7));
            player.startBattle();

            List<Ship_Placement> heavy = new ArrayList<>();
            heavy.add(new Ship_Placement(new Nagato(), new Point(0, 0), true));
            float heavyHit = FleetCalculation.damageBetween(heavy, player.getAlivePlacements(),
                    1f, new ModifiedStats().shieldModifier());
            int full = Math.max(1, Math.round(heavyHit));
            int half = Math.max(1, Math.round(heavyHit / 2f));

            int ddMax = player.getShipMaxHP(dd);
            player.applyShots(pts("A1", "B1"), heavy);
            boolean crippled = player.currentHP(dd) <= ddMax / 2 && player.currentHP(dd) > 0;
            check("two hits cripple the destroyer (" + player.currentHP(dd) + "/" + ddMax + ")",
                    crippled);
            check("so both of its hit tiles are open to re-fire",
                    player.refireTargets().containsAll(pts("A1", "B1")));

            int bbBefore = player.currentHP(bb);
            player.applyShots(pts("A8"), heavy);
            check("one hit on the healthy battleship is not a re-fire target",
                    !player.refireTargets().contains(pts("A8").get(0)));
            int bbAfterOne = player.currentHP(bb);
            player.applyShots(pts("A8"), heavy);
            eq("so firing there again is a wasted shot", player.currentHP(bb), bbAfterOne);
            check("(the first hit did full damage)", bbBefore - bbAfterOne == full);

            int ddBefore = player.currentHP(dd);
            List<ShotOutcome> refire = player.applyShots(pts("A1"), heavy);
            eq("re-firing on the crippled destroyer does half damage",
                    player.currentHP(dd), Math.max(0, ddBefore - half));

            // A tough ship with every tile hit, still above half, is open to re-fire too.
            List<Ship_Placement> light = new ArrayList<>();
            light.add(new Ship_Placement(new I_556(), new Point(0, 0), true));
            player.applyShots(pts("B8", "C8", "D8"), light);
            boolean aboveHalf = player.currentHP(bb) > player.getShipMaxHP(bb) / 2;
            check("a battleship with every tile hit" + (aboveHalf ? ", still above half," : "")
                            + " is open to re-fire on all four",
                    player.refireTargets().containsAll(pts("A8", "B8", "C8", "D8")));
            System.out.println("        outcome of the re-fire: " + refire);

            // Measured on a hull big enough to survive it, so full and half damage differ.
            float lightHit = FleetCalculation.damageBetween(light, player.getAlivePlacements(),
                    1f, new ModifiedStats().shieldModifier());
            int lightHalf = Math.max(1, Math.round(lightHit / 2f));
            int bbBeforeRefire = player.currentHP(bb);
            player.applyShots(pts("A8"), light);
            eq("the enemy's re-fire does exactly half damage (" + lightHalf + ")",
                    bbBeforeRefire - player.currentHP(bb), lightHalf);
        });

        header("24. Both opponents finish off a ship that needs re-firing");
        // One 2-tile ship with 10 hull, 4 per fresh hit and 2 per re-fire: two fresh hits
        // leave it on 2, so only a re-fire can sink it.
        // Averaged over many games: the opponents roll their own dice, so one game each
        // says little about which is quicker.
        int games = 200;
        boolean normalSinks = true;
        boolean eliteSinks = true;
        long normalTotal = 0;
        long eliteTotal = 0;
        for (int g = 0; g < games; g++) {
            int n = simRefire(new NormalEnenmy(GameLayout.SIZE), g);
            int e = simRefire(new EliteEnemy(GameLayout.SIZE), g);
            normalSinks &= n > 0;
            eliteSinks &= e > 0;
            normalTotal += n;
            eliteTotal += e;
        }
        System.out.printf("        average turns to sink over %d games - Standard %.1f, Elite %.1f%n",
                games, normalTotal / (double) games, eliteTotal / (double) games);
        check("the Standard opponent re-fires and sinks it, every game", normalSinks);
        check("the Elite opponent re-fires and sinks it, every game", eliteSinks);
        check("and the Elite one is quicker about it on average", eliteTotal < normalTotal);

        SwingUtilities.invokeAndWait(() -> {
            header("25. Every ship has its own picture, with the hull's as a stand-in");
            Set<String> paths = new HashSet<>();
            boolean allDeclare = true;
            boolean allResolve = true;
            for (Ships.vessels.VesselRegistry.Entry e : Ships.vessels.VesselRegistry.all()) {
                Ships.AbstractShip ship = (Ships.AbstractShip) e.create();
                String own = ship.getVesselImage();
                allDeclare &= own != null && paths.add(own);
                allResolve &= com.bb.Assets.getResource(ship.getImage()) != null;
            }
            check("every vessel names a picture of its own, and no two share one", allDeclare);
            check("and every one shows something - its own file, or its hull's", allResolve);

            // Dropping a file in is all it takes: prove it with a throwaway vessel and file.
            java.net.URL hullArt = com.bb.Assets.getResource("/ships/BattleShip_Class.png");
            if (hullArt != null && "file".equals(hullArt.getProtocol())) {
                try {
                    java.io.File dir = new java.io.File(
                            new java.io.File(hullArt.toURI()).getParentFile(), "vessels");
                    java.io.File probe = new java.io.File(dir, "__selftest_probe__.png");
                    Ships_Type before = new Ships.Battleship("Probe", new ShipStats().hp(1)
                            .size(4).cost(1).image("/ships/vessels/__selftest_probe__.png")) {};
                    eq("with no file yet, the hull's picture stands in",
                            before.getImage(), "/ships/BattleShip_Class.png");
                    dir.mkdirs();
                    java.nio.file.Files.copy(new java.io.File(hullArt.toURI()).toPath(),
                            probe.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Ships_Type after = new Ships.Battleship("Probe", new ShipStats().hp(1)
                            .size(4).cost(1).image("/ships/vessels/__selftest_probe__.png")) {};
                    eq("once the file is there, the ship shows its own",
                            after.getImage(), "/ships/vessels/__selftest_probe__.png");
                    java.nio.file.Files.deleteIfExists(probe.toPath());
                } catch (Exception ex) {
                    check("drop-in picture check (" + ex + ")", false);
                }
            }
        });

        SwingUtilities.invokeAndWait(() -> {
            header("26. Rerolls: reward screen and shop");
            RunState.startNewRun();
            freshBoard(player);
            player.autoDeploy();
            player.setActiveSkills(new ArrayList<>());
            RunState.current().setStage(Rarity.LEGENDARY.getUnlockStage());   // every tier open
            Ship_Placement dented = player.getDeployedPlacements().get(0);
            player.restoreHull(dented, player.getShipMaxHP(dented) / 2, player.getShipMaxHP(dented));

            // The rule itself, over many tables.
            Random dice = new Random(5);
            boolean sameKindAndRarity = true;
            boolean freshOnly = true;
            boolean nullOnlyWhenNothing = true;
            int into = 0;
            int nothing = 0;
            for (int i = 0; i < 3000; i++) {
                List<Reward> table = Reward.roll(player, 3, dice);
                Set<String> seen = new HashSet<>();
                Set<Class<?>> onTable = new HashSet<>();
                for (Reward r : table) {
                    seen.add(r.key());
                    if (r.getSkill() != null) onTable.add(r.getSkill().getClass());
                }
                Reward current = table.get(dice.nextInt(table.size()));
                boolean anyLeft = false;
                if (current.getKind() == Reward.Kind.SKILL) {
                    for (Skills sk : Skills_Register.getAllSkills()) {
                        anyLeft |= sk.rarity() == current.getRarity() && !onTable.contains(sk.getClass());
                    }
                }
                Reward next = Reward.reroll(current, player, seen, dice);
                if (next == null) {
                    nothing++;
                    nullOnlyWhenNothing &= !anyLeft;
                } else {
                    into++;
                    sameKindAndRarity &= next.getKind() == current.getKind()
                            && next.getRarity() == current.getRarity();
                    freshOnly &= next.getSkill() != null && !onTable.contains(next.getSkill().getClass());
                }
            }
            System.out.println("        " + into + " rerolls found a card, " + nothing + " had none");
            check("a rerolled card keeps its kind and rarity", into > 0 && sameKindAndRarity);
            check("and never becomes a card already on the table", freshOnly);
            check("it comes up empty only when that rarity has nothing else", nothing > 0
                    && nullOnlyWhenNothing);
            check("Extra Salvo has nothing to reroll into - not even Drydock Repair",
                    Reward.reroll(Reward.moreShots(), player, new HashSet<>(), dice) == null);

            // The screen: one free reroll, spent on any card.
            RewardPanel panel = (RewardPanel) card(frames, RewardPanel.class);
            RunState.current().setCurrency(100);   // something to charge, were it charged
            int gold = RunState.current().getCurrency();
            List<Reward> before = null;
            int at = -1;
            int other = -1;
            for (int attempt = 0; attempt < 500 && other < 0; attempt++) {
                panel.present(player, 0);
                before = panel.getOptions();
                at = -1;
                for (int i = 0; i < before.size() && at < 0; i++) {
                    if (panel.reroll(i) == null) at = i;
                }
                if (at < 0) continue;
                // A card that could still reroll if there were no one-per-screen cap.
                Set<String> shown = new HashSet<>();
                for (Reward r : before) shown.add(r.key());
                shown.add(panel.getOptions().get(at).key());
                for (int j = 0; j < before.size(); j++) {
                    if (j != at && Reward.canReroll(panel.getOptions().get(j), player, shown)) other = j;
                }
            }
            check("a reward card can be rerolled", at >= 0);
            Reward was = before.get(at);
            Reward now = panel.getOptions().get(at);
            check("into another card of the same kind and rarity (" + was.getTitle() + " -> "
                            + now.getTitle() + ")",
                    !now.key().equals(was.key()) && now.getKind() == was.getKind()
                            && now.getRarity() == was.getRarity());
            boolean othersKept = true;
            for (int j = 0; j < before.size(); j++) {
                if (j != at) othersKept &= panel.getOptions().get(j) == before.get(j);
            }
            check("the other cards stay put", othersKept);
            eq("it is free", RunState.current().getCurrency(), gold);
            eq("and there is one per screen", panel.getRerollsLeft(), 0);
            check("a second one is refused, even on a card that has something to roll into",
                    other >= 0 && panel.reroll(other) != null
                            && panel.getOptions().get(other) == before.get(other));
            List<JButton> used = buttonsNamed(panel, "Reroll used");
            boolean allOff = used.size() == before.size();
            for (JButton b : used) allOff &= !b.isEnabled();
            check("and every card's button says so", allOff);

            String gone = was.key();
            boolean cameBack = false;
            for (int attempt = 0; attempt < 400 && !cameBack; attempt++) {
                panel.present(player, 0);
                for (Reward r : panel.getOptions()) cameBack |= r.key().equals(gone);
            }
            eq("the next screen gets its own reroll", panel.getRerollsLeft(), 1);
            check("and what was rerolled away can be offered there again", cameBack);

            // ...and rolled into again: rerolling a card of its rarity on a later screen.
            boolean rolledBack = false;
            for (int attempt = 0; attempt < 400 && !rolledBack; attempt++) {
                panel.present(player, 0);
                List<Reward> table = panel.getOptions();
                boolean onTable = false;
                for (Reward r : table) onTable |= r.key().equals(gone);
                if (onTable) continue;
                for (int j = 0; j < table.size(); j++) {
                    if (table.get(j).getKind() == was.getKind() && table.get(j).getRarity() == was.getRarity()
                            && panel.reroll(j) == null) {
                        rolledBack = panel.getOptions().get(j).key().equals(gone);
                        break;
                    }
                }
            }
            check("or rerolled into there - the last screen's rerolls don't carry over", rolledBack);

            // The shop: priced 5, 10, 15, 20..., and a slot keeps its hull class.
            RunState.current().setStage(4);
            RunState.current().setCurrency(100000);
            int shopBudget = RunState.current().getDeploymentBudget();
            Shop shop = new Shop(new Random(21));
            int slot = -1;
            for (int i = 0; i < shop.getShipsForSale().size() && slot < 0; i++) {
                if (shop.shipRerollProblem(i) == null) slot = i;
            }
            check("a ship in the shop can be rerolled", slot >= 0);
            eq("the first reroll of a visit costs 5", shop.getRerollPrice(), 5);
            List<Integer> paid = new ArrayList<>();
            boolean hullHeld = true;
            int cameAgain = 0;
            String hull = shop.getShipsForSale().get(slot).getHullCode();
            for (int k = 0; k < 40; k++) {
                int goldBefore = RunState.current().getCurrency();
                String name = shop.getShipsForSale().get(slot).getName();
                hullHeld &= shop.rerollShip(slot) == null;
                Ships_Type got = shop.getShipsForSale().get(slot);
                hullHeld &= got.getHullCode().equals(hull) && got.getCost() <= shopBudget;
                if (got.getName().equals(name)) cameAgain++;
                paid.add(goldBefore - RunState.current().getCurrency());
            }
            eq("each reroll costs 5 more than the last", paid.subList(0, 4), Arrays.asList(5, 10, 15, 20));
            eq("all the way up", paid.get(39), 200);
            check("a " + hull + " slot stays a " + hull, hullHeld);
            check("and can hand back the very ship it replaced (" + cameAgain + " of 40)",
                    cameAgain > 0);

            Set<Shop.Expansion> sizes = new HashSet<>();
            int priceBefore = shop.getRerollPrice();
            for (int k = 0; k < 30; k++) {
                checkQuiet(shop.rerollExpansion() == null);
                sizes.add(shop.getExpansion());
            }
            check("rerolling the fleet expansion rolls its rarity afresh (" + sizes.size()
                    + " sizes seen)", sizes.size() > 1);
            eq("on the same climbing price", shop.getRerollPrice(), priceBefore + 30 * 5);

            RunState.current().setCurrency(shop.getRerollPrice() - 1);
            Ships_Type kept = shop.getShipsForSale().get(slot);
            int priceKept = shop.getRerollPrice();
            check("short of gold, the reroll is refused and nothing changes",
                    shop.rerollShip(slot) != null && shop.getShipsForSale().get(slot) == kept
                            && shop.getRerollPrice() == priceKept
                            && RunState.current().getCurrency() == priceKept - 1);

            RunState.current().setCurrency(100000);
            shop.buyShip(slot, player);
            check("a sold slot cannot be rerolled", shop.rerollShip(slot) != null
                    && shop.getShipsForSale().get(slot) == kept);
            shop.buyExpansion();
            Shop.Expansion bought = shop.getExpansion();
            check("nor a bought expansion", shop.rerollExpansion() != null
                    && shop.getExpansion() == bought);
            eq("a new visit starts back at 5", new Shop(new Random(1)).getRerollPrice(), 5);

            // A slot whose class has nothing else that fits is not charged for a no-op.
            boolean found = false;
            for (int stage = 1; stage <= 12 && !found; stage++) {
                RunState.current().setStage(stage);
                RunState.current().setBudgetBonus(0);
                int b = RunState.current().getDeploymentBudget();
                for (int sd = 0; sd < 300 && !found; sd++) {
                    Shop probe = new Shop(new Random(sd));
                    for (int i = 0; i < probe.getShipsForSale().size() && !found; i++) {
                        String code = probe.getShipsForSale().get(i).getHullCode();
                        int n = 0;
                        for (VesselRegistry.Entry e : VesselRegistry.byHull(code)) {
                            if (e.getCost() <= b) n++;
                        }
                        if (n != 1) continue;
                        found = true;
                        RunState.current().setCurrency(1000);
                        check(probe.getShipsForSale().get(i).getName() + ", the only " + code
                                        + " within " + b + ", cannot be rerolled, and costs nothing",
                                probe.rerollShip(i) != null && RunState.current().getCurrency() == 1000);
                    }
                }
            }
            if (!found) System.out.println("        (no hull class has a lone ship at any budget - skipped)");

            // The shop screen shows the price on every button and moves it on.
            RunState.current().setStage(4);
            RunState.current().setCurrency(1000);
            ShopPanel shopScreen = frames.getShopPanel();
            shopScreen.open(new Shop(new Random(3)));
            int offers = shopScreen.getShop().getShipsForSale().size() + 1;
            eq("every offer has a reroll button at 5", buttonsNamed(shopScreen, "Reroll   5").size(), offers);
            buttonsNamed(shopScreen, "Reroll   5").get(0).doClick();
            eq("after one, they all read 10", buttonsNamed(shopScreen, "Reroll   10").size(), offers);
            eq("and 5 gold is gone", RunState.current().getCurrency(), 995);
        });

        SwingUtilities.invokeAndWait(() -> {
            header("27. Your ships are drawn as pictures; the ship stats board");
            RunState.startNewRun();
            RunState.current().setBudgetBonus(20);
            freshBoard(player);
            showCard(frames, "PLAYER");
            layOut(frames);
            List<Skills> plating = new ArrayList<>();
            plating.add(new modify_shield());
            player.setActiveSkills(plating);

            Ship_Placement cruiser = findInRoster(player, "CB");
            Ship_Placement bb = findInRoster(player, "BB");
            Ship_Placement sub = findInRoster(player, "SS");
            player.beginCarryAt(cruiser, new Point(1, 1));
            player.endCarryAt(new Point(1, 1));                  // B2-D2
            player.beginCarryAt(bb, new Point(0, 4));
            player.endCarryAt(new Point(0, 4));                  // A5-D5
            player.beginCarryAt(sub, new Point(0, 6));
            player.endCarryAt(new Point(0, 6));                  // A7-B7
            check("three ships deployed for the test",
                    player.isDeployed(cruiser) && player.isDeployed(bb) && player.isDeployed(sub));

            // While deploying: right-click looks, a left press still picks the ship up.
            mouse(cellAt(player, "C2"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3);
            eq("right-clicking a ship while deploying opens its stats", player.getStatsShip(), cruiser);
            mouse(cellAt(player, "A5"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            check("a left press still picks the ship up, and puts the stats away",
                    player.isCarrying() && player.getStatsShip() == null);
            // In hand, the picture is drawn faintly over the green landing tiles. Counted away
            // from A5, the tile the press is on, so nothing about the press itself can pass.
            int preview = countUnlike(renderGrid(player), tiles(player, "B5", "D5"),
                    new Color(150, 220, 150), new Color(180, 180, 180));
            Rectangle pressedTile = tiles(player, "A5", "A5");
            int corner = renderGrid(player).getRGB(pressedTile.x + 5, pressedTile.y + 5);
            boolean landingGreen = Math.abs(((corner >> 16) & 255) - 150) <= 6
                    && Math.abs(((corner >> 8) & 255) - 220) <= 6 && Math.abs((corner & 255) - 150) <= 6;
            mouse(cellAt(player, "A5"), MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1);
            check("and letting go puts it back", player.isDeployed(bb)
                    && new Point(0, 4).equals(bb.getOrigin()));
            int[] placed = pixels(renderGrid(player), tiles(player, "A5", "D5"));
            check("the ship in hand is previewed over its landing tiles (" + preview + " px)",
                    preview > 200);
            check("and drawn solid once it is down (alpha " + placed[0] + ")",
                    placed[0] >= 250 && placed[1] > 200);
            check("the tile under the press shows the landing colour, not a pressed-button grey",
                    landingGreen);

            // Dragged over another ship, the overlap shows red through its see-through tiles.
            player.beginCarryAt(sub, new Point(1, 4));           // B5-C5, on top of the battleship
            Color bad = new Color(235, 150, 150);
            check("a drop onto another ship previews red, even over its see-through tiles",
                    cellAt(player, "B5").isOpaque() && cellAt(player, "B5").getBackground().equals(bad)
                            && cellAt(player, "C5").isOpaque() && cellAt(player, "C5").getBackground().equals(bad));
            player.endCarryAt(new Point(1, 4));
            check("and letting go there puts the submarine back where it was",
                    new Point(0, 6).equals(sub.getOrigin()) && !cellAt(player, "B5").isOpaque());

            player.startBattle();
            check("pressing Start drops the deployment pick, so no outline is left over",
                    player.getSelected() == null);
            List<Ship_Placement> firing = new ArrayList<>();
            firing.add(new Ship_Placement(new Montana(), new Point(0, 0), true));

            // Ship tiles are see-through with the picture over them; hits change nothing.
            int[] afloat = pixels(renderGrid(player), tiles(player, "B2", "D2"));
            int[] bbBefore = region(renderGrid(player), tiles(player, "A5", "D5"));
            player.applyShots(pts("B5"), firing);
            check("a hit leaves the ship's picture exactly as it was",
                    player.currentHP(bb) < player.getShipMaxHP(bb)
                            && Arrays.equals(region(renderGrid(player), tiles(player, "A5", "D5")), bbBefore));

            // The report: a ship sunk before every section was hit.
            player.restoreHull(cruiser, 1, player.getShipMaxHP(cruiser));
            player.applyShots(pts("C2"), firing);
            eq("one hit on C2 sinks the cruiser", player.currentHP(cruiser), 0);
            boolean seeThrough = true;
            for (String t : new String[]{"B2", "C2", "D2", "A5", "B5"}) {
                seeThrough &= !cellAt(player, t).isOpaque() && cellAt(player, t).getText().isEmpty();
            }
            check("ship tiles are see-through - hit or not, sunk or not", seeThrough);
            int[] wreck = pixels(renderGrid(player), tiles(player, "B2", "D2"));
            System.out.println("        cruiser afloat: alpha " + afloat[0] + ", " + afloat[2]
                    + " coloured px; sunk: alpha " + wreck[0] + ", " + wreck[2] + " coloured px");
            check("afloat, its picture is solid and in colour", afloat[0] >= 250 && afloat[2] > 100);
            check("sunk, the whole ship turns into a grey, see-through wreck",
                    wreck[1] > 200 && wreck[0] <= 160 && wreck[2] == 0);

            player.applyShots(pts("H8"), firing);
            check("open water stays white, and a miss dark grey",
                    cellAt(player, "H1").isOpaque() && cellAt(player, "H1").getBackground().equals(Color.WHITE)
                            && cellAt(player, "H8").isOpaque()
                            && cellAt(player, "H8").getBackground().equals(Color.DARK_GRAY));

            Submarine boat = (Submarine) sub.getShip();
            if (boat.isSurfaced()) boat.dive();
            int[] under = pixels(renderGrid(player), tiles(player, "A7", "B7"));
            boat.surface();
            int[] up = pixels(renderGrid(player), tiles(player, "A7", "B7"));
            boat.dive();
            check("a submerged submarine is drawn faint (alpha " + under[0] + "), a surfaced one solid ("
                    + up[0] + ")", under[1] > 20 && under[0] <= 125 && up[0] >= 250);

            // A crippled battleship, clicked during the battle.
            int bbMax = player.getShipMaxHP(bb);
            player.restoreHull(bb, bbMax * 2 / 5, bbMax);
            player.restoreIncoming(4, 1, true);                  // B5 hit
            mouse(cellAt(player, "C5"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            ShipStatsCard statsCard = player.getStatsCard();
            eq("clicking a ship in battle opens its stats board", player.getStatsShip(), bb);
            check("and shows it", statsCard.isVisible());
            Rectangle around = tiles(player, "A5", "D5");
            around.grow(2, 2);
            int goldOpen = pixels(renderGrid(player), around)[3];
            eq("titled with its name", statsCard.getTitle(), bb.getShip().getName());
            eq("its hull bar reads the ship's current hull", statsCard.getBar().getValue(),
                    player.currentHP(bb));
            eq("out of its maximum", statsCard.getBar().getMax(), bbMax);
            check("it says the ship is crippled (" + statsCard.getCondition() + ")",
                    statsCard.getCondition().startsWith("Crippled"));
            check("and how much of it is hit (" + statsCard.getWhere() + ")",
                    statsCard.getWhere().contains("1 of " + bb.getShip().getSize() + " sections hit"));
            int shields = Math.round(bb.getShip().getShields() * new ModifiedStats().shieldModifier());
            boolean boosted = false;
            for (String text : labelTexts(statsCard)) {
                boosted |= text.contains(shields + " ") && text.contains("(+");
            }
            check("its shields are shown with Ablative Plating applied (" + shields + ")", boosted);

            player.repairShip(bb);
            eq("an open board keeps up with a repair", statsCard.getBar().getValue(), bbMax);
            eq("and says so", statsCard.getCondition(), "Full hull");

            mouse(cellAt(player, "A5"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            check("clicking the same ship again closes it",
                    player.getStatsShip() == null && !statsCard.isVisible());
            player.setSelected(null);
            int goldShut = pixels(renderGrid(player), around)[3];
            check("the ship on the board gets a gold outline while its board is open ("
                    + goldOpen + " px), and loses it after (" + goldShut + ")",
                    goldOpen > 20 && goldShut == 0);
            mouse(cellAt(player, "A5"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            player.getActionMap().get("closeStats").actionPerformed(null);
            check("Esc closes it", player.getStatsShip() == null && !statsCard.isVisible());
            mouse(cellAt(player, "A5"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            mouse(cellAt(player, "H1"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            check("so does clicking open water", player.getStatsShip() == null);

            mouse(cellAt(player, "B2"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            check("a sunk ship's board says it is sunk",
                    player.getStatsShip() == cruiser && statsCard.getCondition().startsWith("Sunk")
                            && statsCard.getBar().getText().equals("SUNK"));
            mouse(cellAt(player, "B7"), MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            boolean oxygen = false;
            for (String text : labelTexts(statsCard)) oxygen |= text.contains("Oxygen");
            check("clicking another ship switches to it, and a submarine shows its oxygen",
                    player.getStatsShip() == sub && oxygen);

            // Ships in port, from the roster.
            Ship_Placement inPort = findInRoster(player, "CV");
            for (JLabel l : rosterLabels(player, inPort.getShip().getName())) {
                mouse(l, MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
            }
            check("clicking a ship in port during the battle opens its board too",
                    player.getStatsShip() == inPort && statsCard.getWhere().startsWith("In port"));
            player.hideStats();

            // The hover tooltip: the basics plus a small hull bar.
            player.restoreHull(bb, bbMax / 2, bbMax);
            player.showStats(bb, null);   // any refresh rebuilds the tooltips
            player.hideStats();
            String tip = cellAt(player, "A5").getToolTipText();
            Color bar = ShipStatsCard.hullColour(bbMax / 2, bbMax);
            check("hovering a ship shows its hull, " + (bbMax / 2) + " / " + bbMax,
                    tip.contains((bbMax / 2) + " / " + bbMax + " HP"));
            check("with a bar in the hull's colour",
                    tip.contains(String.format("bgcolor=#%02x%02x%02x", bar.getRed(), bar.getGreen(), bar.getBlue())));
            check("alongside detection and size", tip.contains("Detection " + bb.getShip().getDetection())
                    && tip.contains("Size " + bb.getShip().getSize()));
            String portTip = rosterLabels(player, inPort.getShip().getName()).get(0).getToolTipText();
            check("ships in port get the same", portTip.contains(" HP") && portTip.contains("bgcolor="));
            player.setActiveSkills(new ArrayList<>());
        });

        System.out.println();
        System.out.println("=====================================");
        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : (failures + " CHECK(S) FAILED"));
        System.out.println("=====================================");
        System.exit(failures == 0 ? 0 : 1);
    }

    // =====================================================================================
    // Lightweight battle simulation, used to compare the two opponents
    // =====================================================================================

    // =====================================================================================
    // Finding things in the real window
    // =====================================================================================

    /** The container every screen is a card in. */
    private static JPanel cards(Frames frames) {
        return (JPanel) frames.getContentPane().getComponent(0);
    }

    private static void showCard(Frames frames, String name) {
        JPanel cards = cards(frames);
        ((CardLayout) cards.getLayout()).show(cards, name);
    }

    /** True when the card holding {@code screen} is the one on display. */
    private static boolean cardShowing(Frames frames, Component screen) {
        for (Component card : cards(frames).getComponents()) {
            if (card == screen || SwingUtilities.isDescendingFrom(screen, card)) {
                return card.isVisible();
            }
        }
        return false;
    }

    /**
     * Lays the window out without showing it, so mouse positions map onto tiles. validate()
     * does nothing for a frame that was never made displayable, hence doing it by hand.
     */
    private static void layOut(Component c) {
        if (!(c instanceof Container)) return;
        ((Container) c).doLayout();
        for (Component child : ((Container) c).getComponents()) layOut(child);
    }

    /** Sends a mouse press or release to {@code c}, in its middle, as a real click would. */
    private static void mouse(JComponent c, int id, int button) {
        int mask = id == MouseEvent.MOUSE_PRESSED
                ? (button == MouseEvent.BUTTON3 ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK)
                : 0;
        c.dispatchEvent(new MouseEvent(c, id, System.currentTimeMillis(), mask,
                Math.max(1, c.getWidth() / 2), Math.max(1, c.getHeight() / 2), 1,
                button == MouseEvent.BUTTON3, button));
    }

    /** Paints the player's grid - the cells, and the ships drawn over them - into an image. */
    private static BufferedImage renderGrid(GameLayout player) {
        Container grid = cellAt(player, "A1").getParent();
        BufferedImage img = new BufferedImage(Math.max(1, grid.getWidth()),
                Math.max(1, grid.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        grid.paint(g);
        g.dispose();
        return img;
    }

    /** The grid area from one tile to another, e.g. "B2" to "D2". */
    private static Rectangle tiles(GameLayout player, String from, String to) {
        return cellAt(player, from).getBounds().union(cellAt(player, to).getBounds());
    }

    /**
     * Over {@code r}: the highest alpha, how many pixels are drawn at all, how many of those
     * are coloured rather than grey, and how many are the outline's gold.
     */
    private static int[] pixels(BufferedImage img, Rectangle r) {
        int maxAlpha = 0, drawn = 0, coloured = 0, gold = 0;
        for (int y = Math.max(0, r.y); y < Math.min(img.getHeight(), r.y + r.height); y++) {
            for (int x = Math.max(0, r.x); x < Math.min(img.getWidth(), r.x + r.width); x++) {
                int p = img.getRGB(x, y);
                int a = p >>> 24, red = (p >> 16) & 255, green = (p >> 8) & 255, blue = p & 255;
                maxAlpha = Math.max(maxAlpha, a);
                if (a <= 30) continue;
                drawn++;
                if (Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) > 12) coloured++;
                if (red > 225 && green > 185 && green < 235 && blue < 140) gold++;
            }
        }
        return new int[]{maxAlpha, drawn, coloured, gold};
    }

    /** Drawn pixels in {@code r} that are none of {@code colours} (within a few levels). */
    private static int countUnlike(BufferedImage img, Rectangle r, Color... colours) {
        int n = 0;
        for (int y = Math.max(0, r.y); y < Math.min(img.getHeight(), r.y + r.height); y++) {
            for (int x = Math.max(0, r.x); x < Math.min(img.getWidth(), r.x + r.width); x++) {
                int p = img.getRGB(x, y);
                if ((p >>> 24) <= 30) continue;
                boolean known = false;
                for (Color c : colours) {
                    known |= Math.abs(((p >> 16) & 255) - c.getRed()) <= 6
                            && Math.abs(((p >> 8) & 255) - c.getGreen()) <= 6
                            && Math.abs((p & 255) - c.getBlue()) <= 6;
                }
                if (!known) n++;
            }
        }
        return n;
    }

    /** The raw pixels of {@code r}, to compare two paintings. */
    private static int[] region(BufferedImage img, Rectangle r) {
        return img.getRGB(r.x, r.y, r.width, r.height, null, 0, r.width);
    }

    /** Every label's text under {@code root}. */
    private static List<String> labelTexts(Container root) {
        List<String> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getText() != null) out.add(((JLabel) c).getText());
            if (c instanceof Container) out.addAll(labelTexts((Container) c));
        }
        return out;
    }

    /** The roster entries for a ship in port, found by the name in their tooltip. */
    private static List<JLabel> rosterLabels(Container root, String name) {
        List<JLabel> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getToolTipText() != null
                    && ((JLabel) c).getToolTipText().contains("<b>" + name + "</b>")) {
                out.add((JLabel) c);
            }
            if (c instanceof Container) out.addAll(rosterLabels((Container) c, name));
        }
        return out;
    }

    /** Fires one of the card container's key-bound actions, as its keystroke would. */
    private static void pressKey(Frames frames, String actionName) {
        Action action = cards(frames).getActionMap().get(actionName);
        if (action != null) {
            action.actionPerformed(
                    new ActionEvent(cards(frames), ActionEvent.ACTION_PERFORMED, actionName));
        }
    }

    /** Every button under {@code root} whose text is exactly {@code text}. */
    private static List<JButton> buttonsNamed(Container root, String text) {
        List<JButton> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JButton && text.equals(((JButton) c).getText())) out.add((JButton) c);
            if (c instanceof Container) out.addAll(buttonsNamed((Container) c, text));
        }
        return out;
    }

    /** Board tiles by coordinate, e.g. pts("A1", "B1"), as (x=column, y=row). */
    private static List<Point> pts(String... coords) {
        List<Point> out = new ArrayList<>();
        for (String c : coords) {
            out.add(new Point(c.charAt(0) - 'A', Integer.parseInt(c.substring(1)) - 1));
        }
        return out;
    }

    /**
     * Pits an opponent against one ship that only a re-fire can sink, under the board's rule:
     * hits on a ship at or below half, or with every tile hit, may be fired on again at half
     * damage. Returns the turns it took, or -1 if it never sank the ship.
     */
    private static int simRefire(EnemyAI ai, long seed) {
        Random rand = new Random(seed);
        int x = rand.nextInt(GameLayout.SIZE - 1);
        int y = rand.nextInt(GameLayout.SIZE);
        List<Point> hull = new ArrayList<>();
        hull.add(new Point(x, y));
        hull.add(new Point(x + 1, y));
        int maxHP = 10;
        int hp = maxHP;
        boolean[][] fired = new boolean[GameLayout.SIZE][GameLayout.SIZE];
        Set<Point> hit = new HashSet<>();

        for (int turn = 1; turn <= 80; turn++) {
            List<Point> refires = new ArrayList<>();
            if (hp > 0 && (hp <= maxHP / 2 || hit.size() == hull.size())) refires.addAll(hit);

            List<ShotOutcome> outcomes = new ArrayList<>();
            for (Point p : ai.generateShots(2, refires)) {
                boolean again = fired[p.y][p.x];
                if (again && !refires.contains(p)) continue;
                fired[p.y][p.x] = true;
                if (!hull.contains(p) || hp <= 0) {
                    outcomes.add(new ShotOutcome(p, false, false, 0));
                    continue;
                }
                hit.add(p);
                hp -= again ? 2 : 4;
                outcomes.add(new ShotOutcome(p, true, hp <= 0, 4));
            }
            ai.reportResults(outcomes);
            if (hp <= 0) return turn;
        }
        return -1;
    }

    /** The card of the given type, e.g. the reward screen. */
    private static Container card(Frames frames, Class<?> type) {
        for (Component c : cards(frames).getComponents()) {
            if (type.isInstance(c)) return (Container) c;
        }
        throw new IllegalStateException("no " + type.getSimpleName() + " card");
    }

    /** An enemy-board tile by its coordinate, e.g. "B4". */
    private static JButton cellAt(Container board, String coord) {
        for (Component c : board.getComponents()) {
            if (c instanceof JButton && coord.equals(((JButton) c).getClientProperty("coord"))) {
                return (JButton) c;
            }
            if (c instanceof Container) {
                JButton found = cellAt((Container) c, coord);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void fire(OpponentPanel opponent) {
        buttonsNamed(opponent, "Fire").get(0).doClick();
    }

    private static void target(OpponentPanel opponent, String coord) {
        cellAt(opponent, coord).doClick();
    }

    private static void confirm(OpponentPanel opponent) {
        buttonsNamed(opponent, "Confirm shots").get(0).doClick();
    }

    /**
     * A clean board with a known roster - one hull of each class - standing in for the
     * starter pick, so a test does not depend on what a player happened to choose. It skips
     * the starter budget on purpose, the way loading a save does.
     */
    private static void freshBoard(GameLayout player) {
        player.resetBoard();
        List<Ship_Placement> roster = new ArrayList<>();
        Ships_Type[] types = {new Nagato(), new Essex(), new Baltimore(), new Helena(),
                new Balao(), new I_556()};
        for (Ships_Type t : types) roster.add(new Ship_Placement(t, null, true));
        player.getFleet().setPlacements(roster);
        player.rebuildRosterFromFleet();
    }

    /** True when there are buttons and every one of them is shown (or hidden) in its slot. */
    private static boolean allVisible(List<JButton> buttons, boolean visible) {
        if (buttons.isEmpty()) return false;
        for (JButton b : buttons) {
            if (b.isVisible() != visible) return false;
        }
        return true;
    }

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
