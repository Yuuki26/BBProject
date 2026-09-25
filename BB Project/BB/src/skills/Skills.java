package skills;

import java.io.Serializable;

/**
 * Common super-type for every skill in the game.
 *
 * <p>Both {@link Attacker_Skills} and {@link Defender_Skills} extend this, which lets the
 * run's loadout and the reward screen hold one mixed list instead of two parallel ones, and
 * lets {@link SkillsRegistry} route a template to the correct registry by type.
 *
 * <h2>Lifetime convention</h2>
 * Both {@link #usage()} and {@link #turns()} use {@code -1} to mean "unlimited". A purely
 * passive skill therefore returns {@code -1} from both and stays active for the whole run.
 * Returning {@code 0} from either means the skill is dead on arrival and will never apply.
 *
 * <h2>Rarity, weight and upgrades</h2>
 * {@link #rarity()}, {@link #weight()} and {@link #upgradeOf()} decide how often the reward
 * roll offers a skill and from which stage. {@link SkillPool} is what reads them.
 */
public interface Skills extends Serializable {

    String getName();

    String getDescription();

    /** Classpath-relative icon path, e.g. {@code "skills/Enhance.png"}. */
    String getImage();

    /**
     * Carried over from the original attacker/defender interfaces. Nothing reads it - reward
     * odds come from {@link #rarity()} and {@link #weight()}.
     */
    int modify_level();

    /** Number of activations, or {@code -1} for unlimited. */
    int usage();

    /** Number of turns the effect lasts once running, or {@code -1} for permanent. */
    int turns();

    /**
     * How rare this skill is, which sets both its default odds and the first stage it can be
     * offered or picked at.
     *
     * <p>Deliberately not defaulted: a new skill cannot compile until someone has decided how
     * rare it should be.
     */
    Rarity rarity();

    /**
     * Relative odds of the reward roll offering this skill, before any upgrade bonus.
     *
     * <p>Defaults to the rarity's weight. Override it to make one skill more or less common
     * than the rest of its tier without moving it to another tier.
     */
    default int weight() {
        return rarity().getBaseWeight();
    }

    /**
     * The skill this one is an upgrade of, or {@code null} when it is not an upgrade.
     *
     * <p>Owning the skill named here makes this one likelier to be offered, and taking this
     * one replaces it in the loadout. Chains are followed, so ERA Gold (an upgrade of Silver,
     * itself an upgrade of Bronze) also counts as an upgrade of Bronze.
     */
    default Class<? extends Skills> upgradeOf() {
        return null;
    }
}
