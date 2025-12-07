package skills;

import com.bb.GameLayout;

public class Rapid_Fire implements Skills {
    @Override
    public String getName() {
        return "Rapid Fire";
    }
    public String getDescription() {
        return "Fire 2 salvos for the cost of 3 turns";
    }

    @Override
    public String getImage() {
        return "Rapid_Fire.png";
    }
    @Override
    public float modify_DMG() {
        return 1f;
    }
    public float  modify_shield () {
        return 1f;
    }
    public float  modify_HP () {
        return 1f;
    }

}
