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
    public void activate(GameLayout ctx) {

    }
    @Override
    public String getImage() {
        return "skills/Rapid_Fire.png";
    }

}
