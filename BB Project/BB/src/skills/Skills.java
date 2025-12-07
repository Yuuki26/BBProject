package skills;

import com.bb.GameLayout;

public interface Skills {
    String getName();
    public String getDescription();
    public String getImage();
    default void activate(GameLayout ctx) {}

    default float modify_DMG() { return 1.0f; }
    default float modify_shield() { return 1.0f; }
    default float modify_HP() { return 1.0f; }
}
