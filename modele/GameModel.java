package modele;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Classe principale du modèle.
 * C'est elle que Main, le contrôleur ou l'IHM peuvent utiliser.
 */
public class GameModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GameState state;
    private final PathFinder pathFinder;
    private final List<PlayedAction> history;
    private final List<PlayedAction> redoStack;
    private final List<Position> player1Path;
    private final List<Position> player2Path;

    public GameModel() {
        this.state = new GameState();
        this.pathFinder = new PathFinder();
        this.history = new ArrayList<>();
        this.redoStack = new ArrayList<>();
        this.player1Path = new ArrayList<>();
        this.player2Path = new ArrayList<>();
        this.player1Path.add(state.getPlayer1().getPosition());
        this.player2Path.add(state.getPlayer2().getPosition());
    }

    public GameModel(GameState state) {
        this.state = state;
        this.pathFinder = new PathFinder();
        this.history = new ArrayList<>();
        this.redoStack = new ArrayList<>();
        this.player1Path = new ArrayList<>();
        this.player2Path = new ArrayList<>();
        this.player1Path.add(state.getPlayer1().getPosition());
        this.player2Path.add(state.getPlayer2().getPosition());
    }

    private GameModel(GameState state, List<Position> player1Path, List<Position> player2Path) {
        this.state = state;
        this.pathFinder = new PathFinder();
        this.history = new ArrayList<>();
        this.redoStack = new ArrayList<>();
        this.player1Path = new ArrayList<>(player1Path);
        this.player2Path = new ArrayList<>(player2Path);
    }

    public GameModel copy() {
        return new GameModel(new GameState(state), player1Path, player2Path);
    }

    public GameState getState() {
        return state;
    }

    /*
     * Applique une action si elle est valide.
     */
    public boolean play(Action action) {
        return play(action, true);
    }

    private boolean play(Action action, boolean clearRedoStack) {
        if (state.isGameOver()) {
            return false;
        }

        if (action.getPlayerId() != state.getCurrentPlayerId()) {
            return false;
        }

        boolean success;

        if (action instanceof MoveAction) {
            success = movePlayer((MoveAction) action);
        } else if (action instanceof WallAction) {
            success = placeWall((WallAction) action);
        } else {
            success = false;
        }

        if (success && clearRedoStack) {
            redoStack.clear();
        }

        if (success && !state.isGameOver()) {
            state.switchTurn();
        }

        return success;
    }

    public boolean movePlayer(MoveAction action) {
        Player player = state.getPlayerById(action.getPlayerId());
        Position target = action.getTarget();

        if (!isMoveValid(player, target)) {
            return false;
        }

        Position previousPosition = player.getPosition();
        player.setPosition(target);
        pathForPlayer(action.getPlayerId()).add(target);
        history.add(PlayedAction.move(action.getPlayerId(), previousPosition, target));
        return true;
    }

    public boolean isMoveValid(Player player, Position target) {
        Board board = state.getBoard();
        Position from = player.getPosition();
        Player opponent = state.getOpponent(player.getId());
        Position opponentPosition = opponent.getPosition();

        if (!board.isInside(target)) return false;
        if (target.equals(opponentPosition)) return false;

        int rowDiff = Math.abs(from.getRow() - target.getRow());
        int colDiff = Math.abs(from.getCol() - target.getCol());

        boolean oneStep = rowDiff + colDiff == 1;
        if (oneStep) {
            return !board.isBlocked(from, target);
        }

        boolean diagonalJump = rowDiff == 1 && colDiff == 1;
        if (diagonalJump) {
            return isDiagonalJumpValid(board, from, opponentPosition, target);
        }

        boolean straightJump = (rowDiff == 2 && colDiff == 0) || (rowDiff == 0 && colDiff == 2);
        if (!straightJump) return false;

        Position jumpedPosition = new Position(
            (from.getRow() + target.getRow()) / 2,
            (from.getCol() + target.getCol()) / 2
        );

        if (!jumpedPosition.equals(opponentPosition)) return false;
        if (board.isBlocked(from, jumpedPosition)) return false;

        return !board.isBlocked(jumpedPosition, target);
    }

    private boolean isDiagonalJumpValid(Board board, Position from, Position opponentPosition, Position target) {
        int opponentRowDiff = opponentPosition.getRow() - from.getRow();
        int opponentColDiff = opponentPosition.getCol() - from.getCol();
        boolean opponentAdjacent = Math.abs(opponentRowDiff) + Math.abs(opponentColDiff) == 1;

        if (!opponentAdjacent) {
            return false;
        }

        if (board.isBlocked(from, opponentPosition)) {
            return false;
        }

        Position straightJumpTarget = new Position(
            opponentPosition.getRow() + opponentRowDiff,
            opponentPosition.getCol() + opponentColDiff
        );
        boolean blockedByBoardEdge = !board.isInside(straightJumpTarget);
        boolean straightJumpBlocked = blockedByBoardEdge
            || (board.isInside(straightJumpTarget)
                && board.isBlocked(opponentPosition, straightJumpTarget));

        if (!straightJumpBlocked) {
            return false;
        }

        boolean targetNextToOpponent = Math.abs(opponentPosition.getRow() - target.getRow())
            + Math.abs(opponentPosition.getCol() - target.getCol()) == 1;

        return targetNextToOpponent && !board.isBlocked(opponentPosition, target);
    }

    public boolean placeWall(WallAction action) {
        Player player = state.getPlayerById(action.getPlayerId());
        Wall wall = action.getWall();

        if (!isWallPlacementValid(player, wall)) {
            return false;
        }

        state.getBoard().addWall(wall, player.getId());
        player.useWall();
        history.add(PlayedAction.wall(action.getPlayerId(), wall));
        return true;
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public boolean undoLastAction() {
        if (history.isEmpty()) {
            return false;
        }

        PlayedAction playedAction = history.remove(history.size() - 1);
        Player player = state.getPlayerById(playedAction.playerId);

        if (playedAction.wall != null) {
            state.getBoard().removeWall(playedAction.wall);
            player.returnWall();
        } else {
            player.setPosition(playedAction.previousPosition);
            removeLastPathPosition(playedAction.playerId, playedAction.targetPosition);
        }

        state.setCurrentPlayerId(playedAction.playerId);
        redoStack.add(playedAction);
        return true;
    }

    public boolean redoLastAction() {
        if (redoStack.isEmpty()) {
            return false;
        }

        PlayedAction playedAction = redoStack.remove(redoStack.size() - 1);
        boolean success = play(playedAction.toAction(), false);
        if (!success) {
            redoStack.add(playedAction);
        }

        return success;
    }

    public boolean isWallPlacementValid(Player player, Wall wall) {
        Board board = state.getBoard();

        if (player.getWallsLeft() <= 0) return false;
        if (!board.isWallInsideBoard(wall)) return false;
        if (board.hasWallConflict(wall)) return false;

        // On pose temporairement le mur pour vérifier que chacun garde un chemin.
        board.addWall(wall);

        boolean player1HasPath = pathFinder.hasPathToGoal(state, state.getPlayer1());
        boolean player2HasPath = pathFinder.hasPathToGoal(state, state.getPlayer2());

        board.removeWall(wall);

        return player1HasPath && player2HasPath;
    }

    public List<MoveAction> getPossibleMoveActions(Player player) {
        List<MoveAction> moves = new ArrayList<>();
        Position position = player.getPosition();

        for (int row = position.getRow() - 2; row <= position.getRow() + 2; row++) {
            for (int col = position.getCol() - 2; col <= position.getCol() + 2; col++) {
                Position target = new Position(row, col);
                if (isMoveValid(player, target)) {
                    moves.add(new MoveAction(player.getId(), target));
                }
            }
        }

        return moves;
    }

    public List<WallAction> getPossibleWallActions(Player player) {
        List<WallAction> actions = new ArrayList<>();

        for (int row = 0; row < Board.WALL_GRID_SIZE; row++) {
            for (int col = 0; col < Board.WALL_GRID_SIZE; col++) {
                Wall horizontal = new Wall(row, col, WallOrientation.HORIZONTAL);
                Wall vertical = new Wall(row, col, WallOrientation.VERTICAL);

                if (isWallPlacementValid(player, horizontal)) {
                    actions.add(new WallAction(player.getId(), horizontal));
                }

                if (isWallPlacementValid(player, vertical)) {
                    actions.add(new WallAction(player.getId(), vertical));
                }
            }
        }

        return actions;
    }

    public int shortestPathLength(Player player) {
        return pathFinder.shortestPathLength(state, player);
    }

    public List<Position> getPlayerPath(int playerId) {
        return Collections.unmodifiableList(pathForPlayer(playerId));
    }

    public List<Position> getWinningPath() {
        Player winner = state.getWinner();
        if (winner == null) {
            return Collections.emptyList();
        }

        return getPlayerPath(winner.getId());
    }

    private List<Position> pathForPlayer(int playerId) {
        if (playerId == 1) {
            return player1Path;
        }

        if (playerId == 2) {
            return player2Path;
        }

        throw new IllegalArgumentException("Id joueur invalide : " + playerId);
    }

    private void removeLastPathPosition(int playerId, Position expectedPosition) {
        List<Position> path = pathForPlayer(playerId);
        if (path.size() <= 1) {
            return;
        }

        int lastIndex = path.size() - 1;
        Position lastPosition = path.get(lastIndex);
        if (expectedPosition == null || lastPosition.equals(expectedPosition)) {
            path.remove(lastIndex);
        }
    }

    public LastAction getLastAction() {
        if (history.isEmpty()) {
            return null;
        }

        PlayedAction action = history.get(history.size() - 1);
        return new LastAction(action.playerId, action.previousPosition, action.targetPosition, action.wall);
    }

    public static class LastAction {
        private final int playerId;
        private final Position previousPosition;
        private final Position targetPosition;
        private final Wall wall;

        private LastAction(int playerId, Position previousPosition, Position targetPosition, Wall wall) {
            this.playerId = playerId;
            this.previousPosition = previousPosition;
            this.targetPosition = targetPosition;
            this.wall = wall;
        }

        public int getPlayerId() {
            return playerId;
        }

        public Position getPreviousPosition() {
            return previousPosition;
        }

        public Position getTargetPosition() {
            return targetPosition;
        }

        public Wall getWall() {
            return wall;
        }

        public boolean isMove() {
            return previousPosition != null && targetPosition != null;
        }

        public boolean isWall() {
            return wall != null;
        }
    }

    private static class PlayedAction implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int playerId;
        private final Position previousPosition;
        private final Position targetPosition;
        private final Wall wall;

        private PlayedAction(int playerId, Position previousPosition, Position targetPosition, Wall wall) {
            this.playerId = playerId;
            this.previousPosition = previousPosition;
            this.targetPosition = targetPosition;
            this.wall = wall;
        }

        private static PlayedAction move(int playerId, Position previousPosition, Position targetPosition) {
            return new PlayedAction(playerId, previousPosition, targetPosition, null);
        }

        private static PlayedAction wall(int playerId, Wall wall) {
            return new PlayedAction(playerId, null, null, wall);
        }

        private Action toAction() {
            if (wall != null) {
                return new WallAction(playerId, wall);
            }

            return new MoveAction(playerId, targetPosition);
        }
    }
}
