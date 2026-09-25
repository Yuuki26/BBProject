package skills;

public class Rapid_Fire implements Attacker_Skills {
    @Override
    public String getName() {
        return "Rapid Fire";
    }

    @Override
    public String getDescription() {
        return "Doubles the shots in every salvo, for 3 turns";
    }

    @Override
    public String getImage() {
        return "skills/Rapid_Fire.png";
    }

    @Override
    public float modify_DMG() {
        return 1f;
    }

    /** The salvo-doubling the name promises; before this the skill did nothing at all. */
    @Override
    public float modify_Shots() {
        return 2f;
    }

    @Override
    public int modify_level() {
        return 1;
    }

    @Override
    public int usage() {
        return 1;
    }

    @Override
    public int turns() {
        return 3;
    }

    @Override
    public Rarity rarity() {
        return Rarity.UNCOMMON;
    }
}
