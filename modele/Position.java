package modele;

import java.io.Serializable;
import java.util.Objects;

/*
 * Représente une case du plateau.
 * row = ligne, col = colonne.
 */
public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    // Ligne de la case. 0 = tout en haut.
    private final int row;

    // Colonne de la case. 0 = tout à gauche.
    private final int col;

    // Crée une position avec une ligne et une colonne.
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    // Retourne une nouvelle position après un déplacement.
    public Position move(Direction direction) {
        return new Position(row + direction.getDeltaRow(), col + direction.getDeltaCol());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Position)) return false;

        Position other = (Position) obj;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
