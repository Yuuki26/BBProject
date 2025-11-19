package Ships;

public class Destroyer implements Ships_Type{
    public int initialShield =2;
    public int initialDMG =2;
    public int initialShots =1;
    public int initialHP=3;
    public int initialPen=1;
    public int initialDetection=1;
    public int Size=1;
    @Override
    public int getShields(
    ) {
        return initialShield;
    }
    public int getDMG(){
        return initialDMG;
    }
    public int getShots(){
        return initialShots;
    }
    public int getHP(){
        return initialHP;
    }
    public int getPenetration(){
        return initialPen;
    }
    public int DetectionRange(){
        return initialDetection;
    }
    public int getSize(){
        return Size;
    }
    public String getImage(){
        return "/BattleShip_Class.png";
    }
}
