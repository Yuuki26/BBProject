package Ships;

public class MotherShip implements Ships_Type {
    public int initialShield =5;
    public int initialDMG =3;
    public int initialShots =3;
    public int initialHP=8;
    public int initialPen=6;
    public int initialDetection=3;
    public int Size=4;
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
        return "/ships/motherShip.png";
    }

}
