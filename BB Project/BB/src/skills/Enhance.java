package skills;

import com.bb.GameLayout;

public class Enhance  implements Attacker_Skills {
    @Override
    public String getName() {
        return "Enhance";
    }

    @Override
    public String getDescription() {
        return "Your Bullets Penetrates More";
    }

    @Override
    public String getImage() {
        return "skills/Enhance.png";
    }

    @Override
    public float modify_DMG() {
        return 1.5f;

    }

    public int usage() {
        return 0;
    }

    public int turns() {
        return 0;
    }

    public int modify_level() {
        return 1;
    }
}

