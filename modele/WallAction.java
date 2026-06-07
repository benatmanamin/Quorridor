package modele;

/*
 * Action de pose d'un mur.
 */
public class WallAction extends Action {
    private static final long serialVersionUID = 1L;

    private final Wall wall;

    public WallAction(int playerId, Wall wall) {
        super(playerId);
        this.wall = wall;
    }

    public Wall getWall() {
        return wall;
    }
}
