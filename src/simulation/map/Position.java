package simulation.map;

import java.util.Objects;

public class Position {
    public int  x, y;
    public Position(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {return "x: " + this.x + ", y: " + this.y;}

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if(o.getClass() == getClass()) {
            return ((Position) o).x == this.x && ((Position) o).y == this.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
        // Alternatively: return 31 * x + y;
    }
}
