package skills;


public class ERA_BRONZE implements Defender_Skills {
    @Override
    public String getName() {
        return "ERA Bronze";
    }
    public String getDescription() {
        return "Blocks 1 salvos for 3 turn";
    }
    @Override
    public String getImage() {
        return "ERA_AMOR-BRONZE.png";
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

    /** The bottom of the ERA line: common, so a run can start one. */
    @Override
    public Rarity rarity() {
        return Rarity.COMMON;
    }
}

