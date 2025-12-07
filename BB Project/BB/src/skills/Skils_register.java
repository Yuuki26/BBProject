package skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Skils_register  {
    private static final List<Skills> skills = new ArrayList<>();

     static {
        // Register all skills here
        skills.add(new Rapid_Fire());
        skills.add(new Enhance());
        skills.add(new ERA_silver());
        skills.add(new ERA_GOLD());
        skills.add(new modify_shield());
        skills.add(new modify_DMG());
        skills.add(new modify_HP());
        // add more as needed
    }

    public static List<Skills> getAllSkills() {

         return Collections.unmodifiableList(skills);
    }

    public static void registerSkill(Skills skill) {
        skills.add(skill);
    }
}

