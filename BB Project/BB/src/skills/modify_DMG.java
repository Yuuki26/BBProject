package skills;

import com.bb.GameLayout;

public class modify_DMG implements Skills {
    @Override
    public String getName() {
        return "Heavy Caliber";
    }

    @Override
    public String getDescription() {
        return "Increases Damage";
    }

    @Override
    public String getImage() {
        return "skills/modify_DMG.png";
    }

    @Override
    public float modify_DMG() {
        return 1.3f;
    }
}
