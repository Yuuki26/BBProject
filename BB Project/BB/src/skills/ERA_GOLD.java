package skills;

public class ERA_GOLD implements Defender_Skills{
    @Override
    public String getName() {
        return "ERA";
    }
    public String getDescription() {
        return "Blocks 3 salvos for 3 turn";
    }
    @Override
    public String getImage() {
        return "skills/ERA_AMOR-GOLD.png";
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
        return 3;
    }
    public int  turns () {
        return 3;
    }
}
