package Ships;

import java.util.ArrayList;
import java.util.List;

public class DefaultFleet {
    public List<Ship_Placement> fleet;
    public DefaultFleet(){
        fleet=new ArrayList<>();
        fleet.add(new Ship_Placement(new BattleShip(),null,true)); //create a fleet first
        fleet.add(new Ship_Placement(new BattleShip(),null,true));//first turn, the fleet will not on the board yet
        fleet.add(new Ship_Placement(new Destroyer(),null,true));
    }
    public List<Ship_Placement> getPlacements() {
        return fleet;
    }
}
