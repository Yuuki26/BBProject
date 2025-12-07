package Ships;

public class BattleShip implements Ships_Type {
    public int initialShield =10;
    public int initialDMG =8;
    public int initialShots =3;
    public int initialHP=12;
    public int initialPen=8;
    public int initialDetection=5;
    public int Size=3;
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
    public String getImage(){
        return "/ships/BattleShip_Class.png";
    }
    public int getSize(){
        return Size;
    }
}
