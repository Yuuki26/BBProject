package skills;

public class modify_DMG implements Attacker_Skills {
    @Override
    public String getName() {
        return "Heavy Caliber";
    }

    @Override
    public String getDescription() {
        return "Increases damage by 30%, permanent";
    }

    @Override
    public String getImage() {
        return "skills/modify_DMG.png";
    }

    @Override
    public float modify_DMG() {
        return 1.3f;
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
