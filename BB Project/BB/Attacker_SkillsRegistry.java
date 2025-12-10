package skills;

import java.util.ArrayList;
import java.util.List;

public class Attacker_SkillsRegistry {
    private static volatile List<Attacker_Skills_Instance> selected = new ArrayList<>();

    private Attacker_SkillsRegistry() {}

    public static synchronized void setSelectedInstances(List<Attacker_Skills_Instance> instances) {
        selected = instances == null ? new ArrayList<>() : new ArrayList<>(instances);
    }

    public static synchronized List<Attacker_Skills_Instance> getSelectedInstances() {
        return new ArrayList<>(selected);
    }

    public static synchronized void tickTurnAll() {
        for (Attacker_Skills_Instance si : selected) si.tickTurn();
        selected.removeIf(si -> !si.isActive());
    }
}