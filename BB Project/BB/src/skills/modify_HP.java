package skills;

public class modify_HP implements Defender_Skills {
    @Override
    public String getName() {
        return "Reinforced Hull";
    }

    @Override
    public String getDescription() {
        return "Significantly increases hull points (x2, permanent)";
    }

    @Override
    public String getImage() {
        return "skills/modify_HP.png";
    }

    @Override
    public float modify_HP() {
        return 2.0f;
    }

    @Override
    public float modify_shield() {
        return 1f;
    }

    @Override
    public int modify_level() {
        return 1;
    }

    /** Passive: unlimited uses, never expires. */
    @Override
    public int usage() {
        return -1;
    }

    @Override
    public int turns() {
        return -1;
    }
}
