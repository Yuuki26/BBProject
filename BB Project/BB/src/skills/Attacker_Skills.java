package skills;
import java.io.Serial;
import java.io.Serializable;

public interface Attacker_Skills extends Serializable {
    String getName();
    String getDescription();
    String getImage();
    float modify_DMG();

    int  modify_level();
    int usage();
    int turns();

}

