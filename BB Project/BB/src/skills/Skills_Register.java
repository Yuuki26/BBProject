package skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Catalogue of every skill template in the game.
 *
 * <p>Returns {@code List<Skills>} rather than {@code List<Object>} so callers can read a
 * name or an icon without casting, and can still use {@code instanceof} to split offensive
 * from defensive when that distinction matters.
 */
public final class Skills_Register {

    private static final List<Skills> skills = new ArrayList<>();

    static {
        // Offensive
        skills.add(new Rapid_Fire());
        skills.add(new Enhance());
        skills.add(new modify_DMG());
        // Defensive
        skills.add(new modify_HP());
        skills.add(new modify_shield());
        skills.add(new ERA_BRONZE());
        skills.add(new ERA_silver());
        skills.add(new ERA_GOLD());
    }

    private Skills_Register() {}

    /** Every registered template, in catalogue order. */
    public static List<Skills> getAllSkills() {
        return Collections.unmodifiableList(skills);
    }

    /** Registers a new template at runtime. */
    public static void registerSkill(Skills skill) {
        if (skill == null) return;
        skills.add(skill);
    }
}
