package modele;

import java.io.Serializable;

/*
 * Représente un joueur : son id, sa position, sa ligne d'arrivée
 * et son nombre de murs restants.
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private Position position;
    private final int goalRow;
    private int wallsLeft;

    public Player(int id, Position position, int goalRow, int wallsLeft) {
        this.id = id;
        this.position = position;
        this.goalRow = goalRow;
        this.wallsLeft = wallsLeft;
    }

    public Player(Player other) {
        this(other.id, other.position, other.goalRow, other.wallsLeft);
    }

    public int getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getGoalRow() {
        return goalRow;
    }

    public int getWallsLeft() {
        return wallsLeft;
    }

    // Retire un mur au joueur quand il en pose un.
    public void useWall() {
        if (wallsLeft <= 0) {
            throw new IllegalStateException("Le joueur n'a plus de murs.");
        }

        wallsLeft--;
    }

    public void returnWall() {
        wallsLeft++;
    }

    // Le joueur gagne quand il atteint sa ligne d'arrivée.
    public boolean hasWon() {
        return position.getRow() == goalRow;
    }
}
