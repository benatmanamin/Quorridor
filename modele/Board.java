package modele;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Plateau du jeu.
 * Il connaît la taille du plateau et les murs déjà posés.
 */
public class Board implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int SIZE = 9;
    public static final int WALL_GRID_SIZE = SIZE - 1;

    private final Set<Wall> walls;
    private final Map<Wall, Integer> wallOwners;

    public Board() {
        this.walls = new HashSet<>();
        this.wallOwners = new HashMap<>();
    }

    public Board(Board other) {
        this.walls = new HashSet<>(other.walls);
        this.wallOwners = new HashMap<>(other.wallOwners);
    }

    public Set<Wall> getWalls() {
        return Collections.unmodifiableSet(walls);
    }

    public boolean isInside(Position position) {
        return position.getRow() >= 0 && position.getRow() < SIZE
            && position.getCol() >= 0 && position.getCol() < SIZE;
    }

    public boolean isWallInsideBoard(Wall wall) {
        return wall.getRow() >= 0 && wall.getRow() < WALL_GRID_SIZE
            && wall.getCol() >= 0 && wall.getCol() < WALL_GRID_SIZE;
    }

    public void addWall(Wall wall) {
        walls.add(wall);
    }

    public void addWall(Wall wall, int playerId) {
        walls.add(wall);
        wallOwners.put(wall, playerId);
    }

    public void removeWall(Wall wall) {
        walls.remove(wall);
        wallOwners.remove(wall);
    }

    public boolean containsWall(Wall wall) {
        return walls.contains(wall);
    }

    public int getWallOwner(Wall wall) {
        Integer owner = wallOwners.get(wall);
        return owner == null ? 0 : owner;
    }

    /*
     * Vérifie si un passage entre deux cases voisines est bloqué par un mur.
     */
    public boolean isBlocked(Position from, Position to) {
        int r1 = from.getRow();
        int c1 = from.getCol();
        int r2 = to.getRow();
        int c2 = to.getCol();

        // Déplacement vertical : un mur horizontal peut bloquer.
        if (c1 == c2 && Math.abs(r1 - r2) == 1) {
            int topRow = Math.min(r1, r2);
            int col = c1;

            return walls.contains(new Wall(topRow, col, WallOrientation.HORIZONTAL))
                || walls.contains(new Wall(topRow, col - 1, WallOrientation.HORIZONTAL));
        }

        // Déplacement horizontal : un mur vertical peut bloquer.
        if (r1 == r2 && Math.abs(c1 - c2) == 1) {
            int row = r1;
            int leftCol = Math.min(c1, c2);

            return walls.contains(new Wall(row, leftCol, WallOrientation.VERTICAL))
                || walls.contains(new Wall(row - 1, leftCol, WallOrientation.VERTICAL));
        }

        return false;
    }

    /*
     * Vérifie si un nouveau mur chevauche ou croise un mur déjà posé.
     */
    public boolean hasWallConflict(Wall newWall) {
        if (walls.contains(newWall)) {
            return true;
        }

        int r = newWall.getRow();
        int c = newWall.getCol();

        if (newWall.getOrientation() == WallOrientation.HORIZONTAL) {
            if (walls.contains(new Wall(r, c - 1, WallOrientation.HORIZONTAL))) return true;
            if (walls.contains(new Wall(r, c + 1, WallOrientation.HORIZONTAL))) return true;
            if (walls.contains(new Wall(r, c, WallOrientation.VERTICAL))) return true;
        } else {
            if (walls.contains(new Wall(r - 1, c, WallOrientation.VERTICAL))) return true;
            if (walls.contains(new Wall(r + 1, c, WallOrientation.VERTICAL))) return true;
            if (walls.contains(new Wall(r, c, WallOrientation.HORIZONTAL))) return true;
        }

        return false;
    }
}
