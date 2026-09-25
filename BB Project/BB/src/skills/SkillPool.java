package skills;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides which skills the reward roll may offer, and how likely each one is.
 *
 * <p>A skill starts from its {@link Skills#weight() weight}. Then:
 * <ul>
 *   <li>before its rarity's {@linkplain Rarity#getUnlockStage() unlock stage} it is never
 *       offered;</li>
 *   <li>it is never offered if the player already has it, or already has an upgrade of it -
 *       ERA Bronze is no use to someone holding ERA Gold;</li>
 *   <li>if it upgrades a skill the player has, its weight is multiplied by
 *       {@link #UPGRADE_WEIGHT_MULTIPLIER}. This is what makes a run lean into the line it has
 *       started: owning ERA Bronze makes ERA Silver come up far more often.</li>
 * </ul>
 *
 * <p>Taking an upgrade replaces what it upgrades rather than sitting beside it - see
 * {@link #replacedBy}.
 */
public final class SkillPool {

    /** How many times likelier an upgrade of an owned skill is to be offered. */
    public static final int UPGRADE_WEIGHT_MULTIPLIER = 4;

    /** Longest upgrade chain followed; stops a mistaken cycle in upgradeOf() from looping. */
    private static final int MAX_CHAIN = 16;

    private SkillPool() {}

    /**
     * Relative odds of offering {@code candidate} to a player on {@code stage} who already has
     * {@code owned}. Zero means it cannot be offered at all.
     */
    public static int weightOf(Skills candidate, int stage, List<Skills> owned) {
        if (candidate == null || !candidate.rarity().isUnlockedAt(stage)) return 0;

        for (Skills have : owned) {
            if (sameSkill(have, candidate) || isUpgradeOf(have, candidate)) return 0;
        }

        int weight = Math.max(0, candidate.weight());
        return replacedBy(candidate, owned).isEmpty()
                ? weight
                : weight * UPGRADE_WEIGHT_MULTIPLIER;
    }

    /** True when {@code candidate} could be offered at all, i.e. its weight is above zero. */
    public static boolean isOfferable(Skills candidate, int stage, List<Skills> owned) {
        return weightOf(candidate, stage, owned) > 0;
    }

    /** The skills in {@code owned} that taking {@code upgrade} would replace. */
    public static List<Skills> replacedBy(Skills upgrade, List<Skills> owned) {
        List<Skills> out = new ArrayList<>();
        for (Skills have : owned) {
            if (isUpgradeOf(upgrade, have)) out.add(have);
        }
        return out;
    }

    /**
     * True when {@code upgrade} is an upgrade of {@code base}, directly or further along a
     * chain.
     */
    public static boolean isUpgradeOf(Skills upgrade, Skills base) {
        if (upgrade == null || base == null) return false;

        Class<? extends Skills> step = upgrade.upgradeOf();
        for (int depth = 0; step != null && depth < MAX_CHAIN; depth++) {
            if (step == base.getClass()) return true;
            Skills next = template(step);
            step = next == null ? null : next.upgradeOf();
        }
        return false;
    }

    /** Same skill, as the loadout counts it: one of each class. */
    private static boolean sameSkill(Skills a, Skills b) {
        return a.getClass() == b.getClass();
    }

    /** The registered template of a skill class, used to follow an upgrade chain. */
    private static Skills template(Class<? extends Skills> type) {
        for (Skills s : Skills_Register.getAllSkills()) {
            if (s.getClass() == type) return s;
        }
        return null;
    }
}
