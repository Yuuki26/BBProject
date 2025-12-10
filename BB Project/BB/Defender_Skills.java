package skills;

import java.io.Serializable;

public interface Defender_Skills extends Serializable {
    String getName();
    String getDescription();
    String getImage();
    float modify_shield();
    float modify_HP();
    int usage();
    int turns();
    int modify_level();

}