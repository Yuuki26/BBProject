package skills;

import java.util.List;

public class ModifiedStats {
    private final List<Skills> skills;

    public ModifiedStats(List<Skills> skills) {
        this.skills = skills;
    }

    public float dmgModifier() {
        if (skills == null || skills.isEmpty()) return 1f;
        float m = 1f;
        for (Skills s : skills) m *= s.modify_DMG();
        return m;
    }

    public float shieldModifier() {
        if (skills == null || skills.isEmpty()) return 1f;
        float m = 1f;
        for (Skills s : skills) m *= s.modify_shield();
        return m;
    }

    public float hpModifier() {
        if (skills == null || skills.isEmpty()) return 1f;
        float m = 1f;
        for (Skills s : skills) m *= s.modify_HP();
        return m;
    }
}
