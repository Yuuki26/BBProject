package skills;

import com.bb.GameLayout;

public class modify_HP implements Defender_Skills {
    @Override
    public String getName() {
        return "up HP";
    }

    @Override
    public String getDescription() {
        return "Significantly increases Hull Points";
    }

    @Override
    public String getImage() {
        return "skills/modify_HP.png";
    }

    @Override
    public float modify_HP() {
        return 2.0f;
    }
    public float modify_shield() {
        return 1f;
    }
    @Override
    public int modify_level() {
        return 1;
    }
    @Override
    public int  usage() {
        return 0;
    }
    public int turns() {
        return 0;
    }

}
