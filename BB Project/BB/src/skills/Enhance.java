package skills;

public class Enhance implements Attacker_Skills {
    @Override
    public String getName() {
        return "Enhance";
    }

    @Override
    public String getDescription() {
        return "Your bullets penetrate more (+50% damage, permanent)";
    }

    @Override
    public String getImage() {
        return "skills/Enhance.png";
    }

    @Override
    public float modify_DMG() {
        return 1.5f;
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

    @Override
    public int modify_level() {
        return 1;
    }
}
