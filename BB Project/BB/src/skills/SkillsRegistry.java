package skills;

import java.util.ArrayList;
import java.util.List;

public final class SkillsRegistry {
    private static volatile List<Skills> selected = new ArrayList<>();

    private SkillsRegistry() {}

    public static synchronized void setSelectedSkills(List<Skills> skills) {
        selected = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
    }

    public static synchronized List<Skills> getSelectedSkills() {
        return new ArrayList<>(selected);
    }
}