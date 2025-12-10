package skills;

import com.bb.GameLayout;

public class ERA_BRONZE implements Defender_Skills{
    @Override
    public String getName() {
        return "ERA";
    }
    public String getDescription() {
        return "Blocks 1 salvos for 3 turn";
    }
    @Override
    public String getImage() {
        return "skills/ERA_AMOR-BRONZE.png";
    }
    @Override
    public float  modify_shield () {
        return 99f;
    }
    public float  modify_HP () {
        return 1f;
    }

    public int  modify_level () {
        return 2;
    }
    public int  usage () {
        return 1;
    }
    public int  turns () {
        return 3;
    }
}

