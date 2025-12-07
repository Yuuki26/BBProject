package skills;

import com.bb.GameLayout;

public class modify_HP implements Skills {
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
}
