package skills;

import java.util.List;

/**
 * Reads the active skill loadout and collapses it into plain multipliers.
 *
 * <p>Every modifier multiplies, so a skill that should not affect a stat returns {@code 1f}
 * for it, and an empty loadout leaves every stat untouched.
 */
public class ModifiedStats {

    public float dmgModifier() {
        float m = 1f;
        List<Attacker_Skills_Instance> inst = Attacker_SkillsRegistry.getSelectedInstances();
        if (inst == null || inst.isEmpty()) return 1f;
        for (Attacker_Skills_Instance si : inst) {
            if (!si.isActive()) continue;
            m *= si.getTemplate().modify_DMG();
        }
        return m;
    }

    /**
     * Multiplier on the number of shots per salvo. This is what makes Rapid Fire actually
     * fire more often instead of only claiming to in its description.
     */
    public float shotsModifier() {
        float m = 1f;
        List<Attacker_Skills_Instance> inst = Attacker_SkillsRegistry.getSelectedInstances();
        if (inst == null || inst.isEmpty()) return 1f;
        for (Attacker_Skills_Instance si : inst) {
            if (!si.isActive()) continue;
            m *= si.getTemplate().modify_Shots();
        }
        return m;
    }

    public float shieldModifier() {
        float m = 1f;
        List<Defender_Skills_Instance> inst = Defender_SkillsRegistry.getSelectedInstances();
        if (inst == null || inst.isEmpty()) return 1f;
        for (Defender_Skills_Instance si : inst) {
            if (!si.isActive()) continue;
            m *= si.getTemplate().modify_shield();
        }
        return m;
    }

    public float hpModifier() {
        float m = 1f;
        List<Defender_Skills_Instance> inst = Defender_SkillsRegistry.getSelectedInstances();
        if (inst == null || inst.isEmpty()) return 1f;
        for (Defender_Skills_Instance si : inst) {
            if (!si.isActive()) continue;
            m *= si.getTemplate().modify_HP();
        }
        return m;
    }
}
