package com.bb;

import Ships.Ship_Placement;
import skills.Rarity;
import skills.SkillPool;
import skills.Skills;
import skills.Skills_Register;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * One pick offered to the player after clearing a stage.
 *
 * <p>A reward is a label plus the change it makes to the run. Everything that can improve a
 * run between stages goes through here, so {@link RewardPanel} only has to render a list and
 * call {@link #apply(GameLayout)} on whichever one was chosen.
 *
 * <p>Ships are not rewards: the {@link Shop} is the only way to get more of them.
 */
public class Reward {

    /** What kind of change this reward makes, which also decides its icon. */
    public enum Kind { SHOTS, REPAIR, SKILL }

    private static final Random RAND = new Random();

    /**
     * Roll weights for the rewards that are not skills; skills weigh in through
     * {@link SkillPool#weightOf}. These sit at an uncommon skill's weight, so the cards are
     * still mostly skills without the fixed options being crowded out.
     */
    public static final int SHOTS_WEIGHT = 60;
    public static final int REPAIR_WEIGHT = 60;

    private final Kind kind;
    private final String title;
    private final String description;
    private final String iconPath;     // may be null, in which case the card draws text only
    private final Skills skill;        // set for SKILL rewards
    private final int shotsDelta;      // set for SHOTS rewards
    private final int weight;          // relative odds of being offered
    private final String replaces;     // SKILL rewards: name of the owned skill it upgrades

    private Reward(Kind kind, String title, String description, String iconPath,
                   Skills skill, int shotsDelta, int weight, String replaces) {
        this.kind = kind;
        this.title = title;
        this.description = description;
        this.iconPath = iconPath;
        this.skill = skill;
        this.shotsDelta = shotsDelta;
        this.weight = weight;
        this.replaces = replaces;
    }

    public Kind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIconPath() { return iconPath; }
    public Skills getSkill() { return skill; }

    /** Relative odds of this reward being drawn for a card. */
    public int getWeight() { return weight; }

    /** The skill's rarity for a SKILL reward; null for everything else. */
    public Rarity getRarity() { return skill == null ? null : skill.rarity(); }

    /** True when taking this upgrades a skill the player already has. */
    public boolean isUpgrade() { return replaces != null; }

    /** Name of the skill this one would replace, or null. */
    public String getReplaces() { return replaces; }

    /**
     * What counts as "the same offer": the skill itself for a skill card, the kind for the
     * rest. Two cards with the same key are never on one screen.
     */
    public String key() {
        return kind == Kind.SKILL ? skill.getClass().getName() : kind.name();
    }

    /** Applies this reward to the run and, where relevant, to the player's board. */
    public void apply(GameLayout player) {
        switch (kind) {
            case SHOTS:
                RunState.current().addBaseShots(shotsDelta);
                break;
            case REPAIR:
                if (player != null) player.repairAll();
                break;
            case SKILL:
                RunState.current().addSkill(skill);
                break;
        }
    }

    // =====================================================================================
    // Factories
    // =====================================================================================

    public static Reward moreShots() {
        return new Reward(Kind.SHOTS, "Extra Salvo",
                "+1 shot on every turn, permanently.", null, null, 1, SHOTS_WEIGHT, null);
    }

    public static Reward repair() {
        return new Reward(Kind.REPAIR, "Drydock Repair",
                "Restore every ship in your fleet to full hull, free.", null, null, 0,
                REPAIR_WEIGHT, null);
    }

    /** A skill card, weighted for the run as it stands right now. */
    public static Reward skill(Skills s) {
        RunState run = RunState.current();
        List<Skills> owned = run.getLoadout();

        List<Skills> replaced = SkillPool.replacedBy(s, owned);
        String replaces = replaced.isEmpty() ? null : replaced.get(0).getName();

        String description = s.getDescription();
        if (replaces != null) {
            description += (description.endsWith(".") ? " " : ". ")
                    + "Upgrades your " + replaces + ".";
        }

        return new Reward(Kind.SKILL, s.getName(), description, s.getImage(), s, 0,
                SkillPool.weightOf(s, run.getStage(), owned), replaces);
    }

    // =====================================================================================
    // Rolling a choice
    // =====================================================================================

    /**
     * Rolls the options offered after clearing a stage.
     *
     * @param player the player's board, inspected for damage
     * @param count  how many distinct options to offer
     */
    public static List<Reward> roll(GameLayout player, int count) {
        return roll(player, count, RAND);
    }

    /**
     * Rolls the options offered after clearing a stage, drawing from {@code rand}.
     *
     * <p>Every candidate carries a weight, and each card is drawn in proportion to it, without
     * repeats. For skills that weight comes from {@link SkillPool}: rarity sets the base odds,
     * a skill whose rarity has not unlocked yet is left out entirely, and an upgrade of a
     * skill the player already has counts several times over. Skills the player owns, and
     * downgrades of them, are left out, and repair only appears when the fleet is actually
     * damaged, so every card on screen does something.
     */
    public static List<Reward> roll(GameLayout player, int count, Random rand) {
        List<Reward> pool = candidates(player);

        List<Reward> picked = new ArrayList<>();
        while (picked.size() < count && !pool.isEmpty()) {
            picked.add(pool.remove(drawIndex(pool, rand)));
        }
        return picked;
    }

    /**
     * Rolls a replacement for {@code current}, for the reward screen's reroll.
     *
     * <p>The replacement is the same kind of reward at the same rarity - an uncommon skill
     * rerolls into another uncommon skill - drawn by the usual weights, and it is never
     * anything in {@code seen}. The screen passes every offer it has shown so far, the card
     * being rerolled included, so nothing rerolled away can come back on that screen.
     *
     * @return the replacement, or null when nothing qualifies - which is always the case for
     *         Extra Salvo and Drydock Repair, as there is only one of each
     */
    public static Reward reroll(Reward current, GameLayout player, Collection<String> seen,
                                Random rand) {
        List<Reward> pool = rerollPool(current, player, seen);
        return pool.isEmpty() ? null : pool.get(drawIndex(pool, rand));
    }

    /** True when {@link #reroll} has something to roll into. */
    public static boolean canReroll(Reward current, GameLayout player, Collection<String> seen) {
        return !rerollPool(current, player, seen).isEmpty();
    }

    private static List<Reward> rerollPool(Reward current, GameLayout player,
                                           Collection<String> seen) {
        List<Reward> pool = candidates(player);
        pool.removeIf(r -> r.kind != current.kind
                || !Objects.equals(r.getRarity(), current.getRarity())
                || r.key().equals(current.key())
                || seen.contains(r.key()));
        return pool;
    }

    /** Everything that could be offered right now, each with a weight above zero. */
    private static List<Reward> candidates(GameLayout player) {
        List<Reward> pool = new ArrayList<>();

        pool.add(moreShots());

        if (player != null && isDamaged(player)) {
            pool.add(repair());
        }

        for (Skills s : Skills_Register.getAllSkills()) {
            pool.add(skill(s));
        }

        // Weight zero means "not now": a locked or already-owned skill, or a fixed option
        // someone has tuned out of the roll.
        pool.removeIf(r -> r.weight <= 0);
        return pool;
    }

    /** Index of one entry of {@code pool}, chosen with probability proportional to weight. */
    private static int drawIndex(List<Reward> pool, Random rand) {
        long total = 0;
        for (Reward r : pool) total += r.weight;

        long ticket = (long) (rand.nextDouble() * total);
        for (int i = 0; i < pool.size(); i++) {
            ticket -= pool.get(i).weight;
            if (ticket < 0) return i;
        }
        return pool.size() - 1;
    }

    /** True when any ship the player owns, on the board or in port, is missing hull. */
    private static boolean isDamaged(GameLayout player) {
        for (Ship_Placement sp : player.getFleet().getPlacements()) {
            if (player.isDamaged(sp)) return true;
        }
        return false;
    }
}
