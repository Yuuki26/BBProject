package skills;

public class ERA_GOLD implements Skills{
    @Override
    public String getName() {
        return "ERA";
    }
    public String getDescription() {
        return "Blocks 3 salvos for 3 turn";
    }
    @Override
    public String getImage() {
        return "ERA_AMOR-GOLD.png";
    }
    @Override
    public float  modify_shield () {
        return 99f;
    }
    public float  modify_HP () {
        return 1f;
    }
    public float  modify_DMG () {
        return 1f;
    }
}
