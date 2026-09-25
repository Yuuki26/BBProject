package skills;

/**
 * Fills the previously unused {@code skills/modify_shield.png} art slot, and gives the
 * defender reward pool a shield option to sit alongside the hull option.
 */
public class modify_shield implements Defender_Skills {
    @Override
    public String getName() {
        return "Ablative Plating";
    }

    @Override
    public String getDescription() {
        return "Increases shields by 60%, permanent";
    }

    @Override
    public String getImage() {
        return "skills/modify_shield.png";
    }

    @Override
    public float modify_shield() {
        return 1.6f;
    }

    @Override
    public float modify_HP() {
        return 1f;
    }

    @Override
    public int modify_level() {
        return 1;
    }

    @Override
    public int usage() {
        return -1;
    }

    @Override
    public int turns() {
        return -1;
    }

    @Override
    public Rarity rarity() {
        return Rarity.UNCOMMON;
    }
}
