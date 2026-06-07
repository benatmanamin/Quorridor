package modele;

/*
 * Action de déplacement d'un pion vers une case cible.
 */
public class MoveAction extends Action {
    private static final long serialVersionUID = 1L;

    private final Position target;

    public MoveAction(int playerId, Position target) {
        super(playerId);
        this.target = target;
    }

    public Position getTarget() {
        return target;
    }
}
