package skills;

import com.bb.GameLayout;

public class ERA_BRONZE implements Skills {
    @Override
    public String getName() {
        return "ERA Bronze";
    }

    @Override
    public String getDescription() {
        return "Blocks 1 salvo for 3 turns";
    }

    @Override
    public String getImage() {
        return "ERA_AMOR-BRONZE.png";
    }

    @Override
    public float modify_shield() {
        return 50f;
    }
}
