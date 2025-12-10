package skills;

import com.bb.GameLayout;

public class Rapid_Fire implements Attacker_Skills {
    @Override
    public String getName() {
        return "Rapid Fire";
    }
    public String getDescription() {
        return "Fire 2 salvos for the cost of 3 turns";
    }

    @Override
    public String getImage() {
        return "skills/Rapid_Fire.png";
    }
    @Override
    public float modify_DMG() {
        return 1f;
    }
    public int modify_level(){
        return 1;
    }
    public int usage(){
        return 0;
    }
    public int turns(){
        return 0;
    }


}
