package skills;

/** Defensive skills: they change how much punishment the player's fleet absorbs. */
public interface Defender_Skills extends Skills {

    /** Multiplier applied to the fleet's effective shields. {@code 1f} means no change. */
    float modify_shield();

    /** Multiplier applied to the fleet's hull points. {@code 1f} means no change. */
    float modify_HP();
}
