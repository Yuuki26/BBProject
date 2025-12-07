package Ships;

import java.awt.Point;
import java.util.List;
import java.util.ArrayList;

public class Ship_Placement {
    private Ships_Type ship;
    private Point origin;
    private boolean horizontal;

    public Ship_Placement(Ships_Type ship, Point origin, boolean horizontal) {
        this.ship = ship;
        this.origin = origin;
        this.horizontal = horizontal;
    }

    public void setOrigin(Point origin) {
        this.origin = origin;
    }

    public Ships_Type getShip() { return ship; }
    public Point getOrigin() { return origin; }
    public boolean isHorizontal() { return horizontal; }
    public void rotate() { horizontal = !horizontal; }

    public List<Point> getOccupiedTiles() {
        List<Point> tiles = new ArrayList<>();
        if(origin==null)return tiles;
        for (int i = 0; i < ship.getSize(); i++) {
            int x = origin.x + (horizontal ? i : 0);
            int y = origin.y + (horizontal ? 0 : i);
            tiles.add(new Point(x, y));
        }
        return tiles;
    }
}
