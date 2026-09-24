package skills;

/** Offensive skills: they change how hard, and how often, the player's salvo lands. */
public interface Attacker_Skills extends Skills {

    /** Multiplier applied to outgoing damage. {@code 1f} means no change. */
    float modify_DMG();

    /**
     * Multiplier applied to the number of shots in a salvo. {@code 1f} means no change.
     *
     * <p>Defaulted so existing implementations that only tweak damage keep compiling.
     */
    default float modify_Shots() {
        return 1f;
    }
}
