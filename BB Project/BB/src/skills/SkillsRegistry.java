package skills;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade over {@link Attacker_SkillsRegistry} and {@link Defender_SkillsRegistry}.
 *
 * <p>Callers hand it one mixed list of templates; it wraps each in the matching
 * {@code *_Skills_Instance} (which owns the remaining-uses / remaining-turns counters) and
 * files it under the right registry. This is the only place that needs to know a skill can
 * be offensive or defensive, so the UI and the run loop stay type-agnostic.
 */
public final class SkillsRegistry {

    private SkillsRegistry() {}

    /** Replaces the active loadout with {@code selected}. A null or empty list clears it. */
    public static synchronized void setSelectedSkills(List<Skills> selected) {
        List<Attacker_Skills_Instance> attackers = new ArrayList<>();
        List<Defender_Skills_Instance> defenders = new ArrayList<>();

        if (selected != null) {
            for (Skills s : selected) {
                if (s instanceof Attacker_Skills) {
                    attackers.add(new Attacker_Skills_Instance((Attacker_Skills) s));
                } else if (s instanceof Defender_Skills) {
                    defenders.add(new Defender_Skills_Instance((Defender_Skills) s));
                }
            }
        }

        Attacker_SkillsRegistry.setSelectedInstances(attackers);
        Defender_SkillsRegistry.setSelectedInstances(defenders);
    }

    /** The templates currently in the loadout, offensive first. */
    public static synchronized List<Skills> getSelectedSkills() {
        List<Skills> all = new ArrayList<>();
        for (Attacker_Skills_Instance si : Attacker_SkillsRegistry.getSelectedInstances()) {
            all.add(si.getTemplate());
        }
        for (Defender_Skills_Instance si : Defender_SkillsRegistry.getSelectedInstances()) {
            all.add(si.getTemplate());
        }
        return all;
    }

    /** Adds one template to the loadout without disturbing what is already there. */
    public static synchronized void addSkill(Skills skill) {
        if (skill == null) return;
        List<Skills> current = getSelectedSkills();
        current.add(skill);
        setSelectedSkills(current);
    }

    /** Advances every active skill by one turn and drops the ones that just expired. */
    public static synchronized void tickTurnAll() {
        Attacker_SkillsRegistry.tickTurnAll();
        Defender_SkillsRegistry.tickTurnAll();
    }

    /** Drops the whole loadout. Used when a run ends. */
    public static synchronized void reset() {
        Attacker_SkillsRegistry.setSelectedInstances(new ArrayList<>());
        Defender_SkillsRegistry.setSelectedInstances(new ArrayList<>());
    }
}
