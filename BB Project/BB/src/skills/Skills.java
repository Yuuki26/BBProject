package skills;

import com.bb.GameLayout;

public interface Skills {
    String getName();
    public String getDescription();
    public String getImage();
    void activate(GameLayout ctx);
}
