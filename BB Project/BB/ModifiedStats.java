package skills;

import java.util.List;


import java.util.List;

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
