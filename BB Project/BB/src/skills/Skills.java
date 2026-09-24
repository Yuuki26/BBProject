package skills;

import java.io.Serializable;

/**
 * Common super-type for every skill in the game.
 *
 * <p>Both {@link Attacker_Skills} and {@link Defender_Skills} extend this, which lets the
 * selection UI ({@code com.bb.Skill_Dialogs}) hold one mixed list instead of two parallel
 * ones, and lets {@link SkillsRegistry} route a template to the correct registry by type.
 *
 * <h2>Lifetime convention</h2>
 * Both {@link #usage()} and {@link #turns()} use {@code -1} to mean "unlimited". A purely
 * passive skill therefore returns {@code -1} from both and stays active for the whole run.
 * Returning {@code 0} from either means the skill is dead on arrival and will never apply.
 */
public interface Skills extends Serializable {

    String getName();

    String getDescription();

    /** Classpath-relative icon path, e.g. {@code "skills/Enhance.png"}. */
    String getImage();

    /** Rarity / power tier, used to weight roguelike reward rolls. */
    int modify_level();

    /** Number of activations, or {@code -1} for unlimited. */
    int usage();

    /** Number of turns the effect lasts once running, or {@code -1} for permanent. */
    int turns();
}
