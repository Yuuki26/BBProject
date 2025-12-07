package skills;

public class modify_shield implements Skills {
    @Override
    public String getName() {
        return "up shield";
    }

    @Override
    public String getDescription() {
        return "Significantly increases Shield Points";
    }

    @Override
    public String getImage() {
        return "skills/modify_shield.png";
    }

    @Override
    public float modify_shield() {
        return 50f;
    }
}
