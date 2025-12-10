package skills;
import skills.Defender_Skills_Instance;

import java.util.ArrayList;
import java.util.List;

public class Defender_SkillsRegistry {
    private static volatile List<Defender_Skills_Instance> selected = new ArrayList<>();

    private Defender_SkillsRegistry() {}

    public static synchronized void setSelectedInstances(List<Defender_Skills_Instance> instances) {
        selected = instances == null ? new ArrayList<>() : new ArrayList<>(instances);
    }

    public static synchronized List<Defender_Skills_Instance> getSelectedInstances() {
        return new ArrayList<>(selected);
    }

    public static synchronized void tickTurnAll() {
        for (Defender_Skills_Instance si : selected) si.tickTurn();
        selected.removeIf(si -> !si.isActive());
    }
}
