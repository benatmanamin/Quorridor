package modele;

import java.io.Serializable;
import java.util.Objects;

/*
 * Représente un mur.
 * Sur un plateau 9x9, les murs se placent sur une grille 8x8.
 */
public class Wall implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;
    private final WallOrientation orientation;

    public Wall(int row, int col, WallOrientation orientation) {
        this.row = row;
        this.col = col;
        this.orientation = orientation;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public WallOrientation getOrientation() {
        return orientation;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Wall)) return false;

        Wall other = (Wall) obj;
        return row == other.row && col == other.col && orientation == other.orientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col, orientation);
    }

    @Override
    public String toString() {
        return orientation + " wall at (" + row + ", " + col + ")";
    }
}
