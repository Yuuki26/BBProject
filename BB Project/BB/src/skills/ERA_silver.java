package skills;

public class ERA_silver implements Defender_Skills{
    @Override
    public String getName() {
        return "ERA";
    }
    public String getDescription() {
        return "Blocks 2 salvos for 3 turn";
    }
    @Override
    public String getImage() {
        return "skills/ERA_AMOR-SILVER.png";
    }
    @Override
    public float  modify_shield () {
        return 99f;
    }
    public float  modify_HP () {
        return 1f;
    }
    public int  modify_level () {
        return 1;
    }
    public int  usage () {
        return 2;
    }
    public int  turns () {
        return 3;
    }
}

