package skills;

public class ERA_GOLD implements Defender_Skills{
    @Override
    public String getName() {
        return "ERA Gold";
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
    public int  modify_level () {
        return 2;
    }
    public int  usage () {
        return 3;
    }
    public int  turns () {
        return 3;
    }

    @Override
    public Rarity rarity() {
        return Rarity.LEGENDARY;
    }

    /**
     * Owning Silver - or Bronze, further down the same line - makes this likelier to be
     * offered; taking it replaces whichever of them the player has.
     */
    @Override
    public Class<? extends Skills> upgradeOf() {
        return ERA_silver.class;
    }
}
