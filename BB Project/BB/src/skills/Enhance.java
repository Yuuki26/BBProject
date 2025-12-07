package skills;

import com.bb.GameLayout;

public class Enhance  implements Skills{
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
    public float  modify_shield () {
        return 1f;
    }
    public float  modify_HP () {
        return 0f;
    }
}
