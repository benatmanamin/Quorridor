package modele;

import java.io.Serializable;

/*
 * Classe mère des actions possibles.
 * Une action appartient toujours à un joueur.
 */
public abstract class Action implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int playerId;

    public Action(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }
}
