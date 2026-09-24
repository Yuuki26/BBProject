package com.bb;

import Ships.vessels.VesselRegistry;
import Ships.Ship_Placement;
import Ships.Ships_Type;
import skills.Skills;
import skills.Skills_Register;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One pick offered to the player after clearing a stage.
 *
 * <p>A reward is a label plus the change it makes to the run. Everything that can improve a
 * run between stages goes through here, so {@link RewardPanel} only has to render a list and
 * call {@link #apply(GameLayout)} on whichever one was chosen.
 */
public class Reward {

    /** What kind of change this reward makes, which also decides its icon. */
    public enum Kind { SHOTS, REPAIR, SKILL, SHIP }

    private static final Random RAND = new Random();

    /** Most expensive hull a stage reward will hand out for free. */
    private static final int MAX_REWARD_HULL_COST = 4;

    private final Kind kind;
    private final String title;
    private final String description;
    private final String iconPath;     // may be null, in which case the card draws text only
    private final Skills skill;        // set for SKILL rewards
    private final Ships_Type ship;     // set for SHIP rewards
    private final int shotsDelta;      // set for SHOTS rewards

    private Reward(Kind kind, String title, String description, String iconPath,
                   Skills skill, Ships_Type ship, int shotsDelta) {
        this.kind = kind;
        this.title = title;
        this.description = description;
        this.iconPath = iconPath;
        this.skill = skill;
        this.ship = ship;
        this.shotsDelta = shotsDelta;
    }

    public Kind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIconPath() { return iconPath; }

    /** Applies this reward to the run and, where relevant, to the player's board. */
    public void apply(GameLayout player) {
        switch (kind) {
            case SHOTS:
                RunState.current().addBaseShots(shotsDelta);
                break;
            case REPAIR:
                if (player != null) player.prepareNextStage(true);
                break;
            case SKILL:
                RunState.current().addSkill(skill);
                break;
            case SHIP:
                if (player != null) player.getFleet().addShip(ship);
                break;
        }
    }

    // =====================================================================================
    // Factories
    // =====================================================================================

    public static Reward moreShots() {
        return new Reward(Kind.SHOTS, "Extra Salvo",
                "+1 shot on every turn, permanently.", null, null, null, 1);
    }

    public static Reward repair() {
        return new Reward(Kind.REPAIR, "Drydock Repair",
                "Restore every ship in your fleet to full hull.", null, null, null, 0);
    }

    public static Reward skill(Skills s) {
        return new Reward(Kind.SKILL, s.getName(), s.getDescription(),
                s.getImage(), s, null, 0);
    }

    public static Reward ship(Ships_Type type) {
        return new Reward(Kind.SHIP, type.getName() + " (" + type.getHullCode() + ")",
                "Adds " + type.getName() + ", a " + type.getHullClass().toLowerCase()
                        + " of cost " + type.getCost() + ", to your roster. "
                        + "You will need to deploy it.",
                type.getImage(), null, type, 0);
    }

    // =====================================================================================
    // Rolling a choice
    // =====================================================================================

    /**
     * Rolls the options offered after clearing a stage.
     *
     * <p>Skills the player already owns are filtered out, and repair is only offered when
     * the fleet is actually damaged, so every card on screen does something.
     *
     * @param player the player's board, inspected for damage and roster size
     * @param count  how many distinct options to offer
     */
    public static List<Reward> roll(GameLayout player, int count) {
        List<Reward> pool = new ArrayList<>();

        pool.add(moreShots());

        if (player != null && isDamaged(player)) {
            pool.add(repair());
        }

        for (Skills s : Skills_Register.getAllSkills()) {
            if (!RunState.current().hasSkill(s.getClass())) {
                pool.add(skill(s));
            }
        }

        // Keep the roster from outgrowing the board, then offer a hull the player can use.
        // Cheap hulls only: a free battleship between stages would flatten the cost budget
        // the whole difficulty curve is built on.
        if (player == null || player.getFleet().totalSize() < GameLayout.SIZE * 3) {
            List<VesselRegistry.Entry> cheap = VesselRegistry.affordable(MAX_REWARD_HULL_COST);
            if (!cheap.isEmpty()) {
                pool.add(ship(cheap.get(RAND.nextInt(cheap.size())).create()));
            }
        }

        Collections.shuffle(pool, RAND);

        // A shot upgrade is always useful, so guarantee at least one meaningful card.
        if (pool.isEmpty()) pool.add(moreShots());

        return new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
    }

    private static boolean isDamaged(GameLayout player) {
        for (Ship_Placement sp : player.getDeployedPlacements()) {
            if (player.getShipHP(sp) < player.getShipMaxHP(sp)) return true;
        }
        return false;
    }
}
