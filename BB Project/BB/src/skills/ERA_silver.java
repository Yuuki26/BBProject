package skills;

public class ERA_silver implements Defender_Skills{
    @Override
    public String getName() {
        return "ERA Silver";
    }
    public String getDescription() {
        return "Blocks 2 salvos for 3 turn";
    }
    @Override
    public String getImage() {
        return "ERA_AMOR-SILVER.png";
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

    @Override
    public Rarity rarity() {
        return Rarity.RARE;
    }

    /** Owning Bronze makes this likelier to be offered; taking it replaces Bronze. */
    @Override
    public Class<? extends Skills> upgradeOf() {
        return ERA_BRONZE.class;
    }
}

