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
    public void activate(GameLayout ctx) {

    }
    @Override
    public String getImage() {
        return "Enhance.png";
    }
}
