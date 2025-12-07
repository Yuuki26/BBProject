package skills;

import java.util.List;

public class ModifiedStats {
    public float dmgModifier() {
        List<Skills> skills = SkillsRegistry.getSelectedSkills();
        if (skills == null || skills.isEmpty()) return 1f;
        float m = 1f;
        for (Skills s : skills) m *= s.modify_DMG();
        return m;
    }

    public float shieldModifier() {
        List<Skills> skills = SkillsRegistry.getSelectedSkills();
        if (skills == null || skills.isEmpty()) return 1f;
        float m = 1f;
        for (Skills s : skills) m *= s.modify_shield();
        return m;
    }

    public float hpModifier() {
        List<Skills> skills = SkillsRegistry.getSelectedSkills();
        if (skills == null || skills.isEmpty()) return 1f;
        float m = 1f;
        for (Skills s : skills) m *= s.modify_HP();
        return m;
    }
}
