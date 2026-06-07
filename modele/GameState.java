package modele;

import java.io.Serializable;

/*
 * Contient tout l'état actuel de la partie :
 * plateau, joueurs, joueur courant, victoire.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Board board;
    private final Player player1;
    private final Player player2;
    private int currentPlayerId;

    public GameState() {
        this.board = new Board();
        this.player1 = new Player(1, new Position(8, 4), 0, 10);
        this.player2 = new Player(2, new Position(0, 4), 8, 10);
        this.currentPlayerId = 1;
    }

    public GameState(GameState other) {
        this.board = new Board(other.board);
        this.player1 = new Player(other.player1);
        this.player2 = new Player(other.player2);
        this.currentPlayerId = other.currentPlayerId;
    }

    public Board getBoard() {
        return board;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Player getPlayerById(int id) {
        if (id == 1) return player1;
        if (id == 2) return player2;
        throw new IllegalArgumentException("Id joueur invalide : " + id);
    }

    public Player getOpponent(int playerId) {
        return playerId == 1 ? player2 : player1;
    }

    public int getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(int currentPlayerId) {
        if (currentPlayerId != 1 && currentPlayerId != 2) {
            throw new IllegalArgumentException("Id joueur invalide : " + currentPlayerId);
        }

        this.currentPlayerId = currentPlayerId;
    }

    public Player getCurrentPlayer() {
        return getPlayerById(currentPlayerId);
    }

    public void switchTurn() {
        currentPlayerId = currentPlayerId == 1 ? 2 : 1;
    }

    public boolean isGameOver() {
        return player1.hasWon() || player2.hasWon();
    }

    public Player getWinner() {
        if (player1.hasWon()) return player1;
        if (player2.hasWon()) return player2;
        return null;
    }
}
