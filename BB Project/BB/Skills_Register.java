package skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Skills_Register {
    private static final List<Object> skills = new ArrayList<>();

    static {
        // Register all skill templates here (attacker and defender implementations)
        // Replace/add entries with your concrete classes
        skills.add(new Rapid_Fire());   // if Rapid_Fire implements Attacker_Skills
        skills.add(new Enhance());      // Attacker Skills
        skills.add(new ERA_silver());   // Defender_Skills
        skills.add(new ERA_GOLD());     // Defender_Skills
        // add more as needed
    }

    private Skills_Register() {}

    /**
     * Returns an unmodifiable list of all skill templates (mixed types).
     * Callers should use instanceof to route templates to attacker/defender UI.
     */
    public static List<Object> getAllSkills() {
        return Collections.unmodifiableList(skills);
    }

    /**
     * Register a new skill template at runtime. Accepts any template object
     * (should implement Attacker_Skills or Defender_Skills).
     */
    public static void registerSkill(Object skill) {
        if (skill == null) return;
        skills.add(skill);
    }
}





