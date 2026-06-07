package ia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import modele.Action;
import modele.Board;
import modele.GameModel;
import modele.GameState;
import modele.MoveAction;
import modele.PathFinder;
import modele.Player;
import modele.Position;
import modele.Wall;
import modele.WallAction;
import modele.WallOrientation;

public class MinimaxAi {
    private static final int WIN_SCORE = 100000;
    private static final int INF = 1000000;
    private static final int RECENT_POSITION_LIMIT = 8;

    private final Random random;
    private final PathFinder pathFinder;
    private final Map<Integer, Position> previousPositions;
    private final Map<Integer, List<Position>> recentPositions;

    
    public MinimaxAi() {
        this.random = new Random();
        this.pathFinder = new PathFinder();
        this.previousPositions = new HashMap<>();
        this.recentPositions = new HashMap<>();
    }

    public Action chooseAction(GameModel model, AiDifficulty difficulty) {

        if (model.getState().isGameOver()) {
            return null;
        }

        int aiPlayerId = model.getState().getCurrentPlayerId();
        Position currentPos = model.getState().getPlayerById(aiPlayerId).getPosition();
        Position previousPos = previousPositions.get(aiPlayerId);
        Action chosenAction;

        List<Action> actions = getCandidateActions(model, aiPlayerId, difficulty);
        chosenAction = chooseBestScoredAction(model, actions, aiPlayerId, difficulty, currentPos, previousPos);

        rememberMove(model, aiPlayerId, currentPos, chosenAction);
        return chosenAction;
    }

    private Action chooseBestScoredAction(GameModel model, List<Action> actions, int aiPlayerId,
            AiDifficulty level, Position currentPos, Position previousPos) {
        if (actions.isEmpty()) {
            return null;
        }

        List<ScoredAction> scoredActions = new ArrayList<>();
        for (Action action : actions) {
            GameModel nextModel = model.copy();
            if (!nextModel.play(action)) {
                continue;
            }

            int score = minimax(nextModel, level.getSearchDepth() - 1, aiPlayerId, level, -INF, INF);
            score = applyActionAdjustments(score, level, model, nextModel,
                aiPlayerId, currentPos, previousPos);
            scoredActions.add(new ScoredAction(action, score));
        }

        if (scoredActions.isEmpty()) {
            return null;
        }

        scoredActions.sort(Comparator.comparingInt(ScoredAction::getScore).reversed());
        int bestScore = scoredActions.get(0).getScore();
        List<ScoredAction> validChoices = new ArrayList<>();
        int tolerance = getChoiceTolerance(level);

        int maxChoices = Math.min(level.getTopChoiceCount(), scoredActions.size());
        for (int i = 0; i < maxChoices; i++) {
            ScoredAction sa = scoredActions.get(i);
            if (bestScore - sa.getScore() <= tolerance) {
                validChoices.add(sa);
            }
        }

        if (validChoices.isEmpty()) {
            validChoices.add(scoredActions.get(0));
        }

        return validChoices.get(random.nextInt(validChoices.size())).getAction();
    }

    private int applyActionAdjustments(int score, AiDifficulty level, GameModel currentModel,
            GameModel nextModel, int aiPlayerId, Position currentPos, Position previousPos) {
        score += tempoScore(currentModel, nextModel, aiPlayerId, level);
        score -= repetitionPenalty(currentModel, nextModel, aiPlayerId);
        return applyBacktrackAdjustment(score, level, currentModel, nextModel,
            aiPlayerId, currentPos, previousPos);
    }

    private int applyBacktrackAdjustment(int score, AiDifficulty level, GameModel currentModel,
            GameModel nextModel, int aiPlayerId,
            Position currentPos, Position previousPos) {
        if (!returnsToPreviousPosition(nextModel, aiPlayerId, currentPos, previousPos)) {
            return score;
        }

        Player currentPlayer = currentModel.getState().getPlayerById(aiPlayerId);
        Player nextPlayer = nextModel.getState().getPlayerById(aiPlayerId);
        boolean improvesShortestPath = normalizedPathLength(nextModel.shortestPathLength(nextPlayer))
            < normalizedPathLength(currentModel.shortestPathLength(currentPlayer));

        return score - getBacktrackPenalty(level, improvesShortestPath);
    }

    private boolean returnsToPreviousPosition(GameModel nextModel, int aiPlayerId,
            Position currentPos, Position previousPos) {
        if (previousPos == null) {
            return false;
        }

        Position resultingPos = nextModel.getState().getPlayerById(aiPlayerId).getPosition();
        boolean isMoveAction = !resultingPos.equals(currentPos);
        return isMoveAction && resultingPos.equals(previousPos);
    }

    private int getBacktrackPenalty(AiDifficulty level, boolean improvesShortestPath) {
        if (improvesShortestPath) return 0;
        if (level == AiDifficulty.ADVANCED) return 120;
        return 80;
    }

    private int repetitionPenalty(GameModel currentModel, GameModel nextModel, int playerId) {
        Position currentPosition = currentModel.getState().getPlayerById(playerId).getPosition();
        Position nextPosition = nextModel.getState().getPlayerById(playerId).getPosition();
        if (currentPosition.equals(nextPosition)) {
            return 0;
        }

        int visits = countRecentVisits(playerId, nextPosition);
        if (visits == 0) {
            return 0;
        }

        Player currentPlayer = currentModel.getState().getPlayerById(playerId);
        Player nextPlayer = nextModel.getState().getPlayerById(playerId);
        int currentPath = normalizedPathLength(currentModel.shortestPathLength(currentPlayer));
        int nextPath = normalizedPathLength(nextModel.shortestPathLength(nextPlayer));
        boolean improvesShortestPath = nextPath < currentPath;

        int penalty = visits * 180;
        return improvesShortestPath ? penalty / 4 : penalty;
    }

    private int countRecentVisits(int playerId, Position position) {
        List<Position> history = recentPositions.get(playerId);
        if (history == null) {
            return 0;
        }

        int visits = 0;
        for (Position recentPosition : history) {
            if (recentPosition.equals(position)) {
                visits++;
            }
        }
        return visits;
    }

    private void rememberMove(GameModel model, int aiPlayerId, Position currentPos, Action chosenAction) {
        GameModel checkModel = model.copy();
        if (!checkModel.play(chosenAction)) {
            return;
        }

        Position posAfterAction = checkModel.getState().getPlayerById(aiPlayerId).getPosition();
        if (!posAfterAction.equals(currentPos)) {
            previousPositions.put(aiPlayerId, currentPos);
            rememberRecentPosition(aiPlayerId, posAfterAction);
        }
    }

    private void rememberRecentPosition(int playerId, Position position) {
        List<Position> history = recentPositions.get(playerId);
        if (history == null) {
            history = new ArrayList<>();
            recentPositions.put(playerId, history);
        }

        history.add(position);
        while (history.size() > RECENT_POSITION_LIMIT) {
            history.remove(0);
        }
    }

    private int getChoiceTolerance(AiDifficulty level) {
        if (level == AiDifficulty.BEGINNER) return 300;
        if (level == AiDifficulty.INTERMEDIATE) return 100;
        return 0;
    }

    private int tempoScore(GameModel currentModel, GameModel nextModel, int playerId, AiDifficulty level) {
        Player currentPlayer = currentModel.getState().getPlayerById(playerId);
        Player nextPlayer = nextModel.getState().getPlayerById(playerId);
        int currentPath = normalizedPathLength(currentModel.shortestPathLength(currentPlayer));
        int nextPath = normalizedPathLength(nextModel.shortestPathLength(nextPlayer));

        boolean isMoveAction = !currentPlayer.getPosition().equals(nextPlayer.getPosition());
        if (isMoveAction) {
            int pathGain = currentPath - nextPath;
            int score = pathGain * (level == AiDifficulty.ADVANCED ? 120 : 70);

            if (pathGain == 0) {
                score -= level == AiDifficulty.ADVANCED ? 90 : 35;
            } else if (pathGain < 0) {
                score -= (-pathGain) * (level == AiDifficulty.ADVANCED ? 180 : 90);
            }

            if (nextPath <= 1) {
                score += 800;
            }

            return score;
        }

        Player currentOpponent = currentModel.getState().getOpponent(playerId);
        Player nextOpponent = nextModel.getState().getOpponent(playerId);
        int currentOpponentPath = normalizedPathLength(currentModel.shortestPathLength(currentOpponent));
        int nextOpponentPath = normalizedPathLength(nextModel.shortestPathLength(nextOpponent));
        int opponentDelay = nextOpponentPath - currentOpponentPath;
        int selfDelay = nextPath - currentPath;

        int score = opponentDelay * (level == AiDifficulty.ADVANCED ? 100 : 60);
        if (selfDelay > 0) {
            score -= selfDelay * 150;
        }
        if (currentPath <= 2 && opponentDelay <= 1) {
            score -= 350;
        }
        if (currentOpponentPath <= 2 && opponentDelay > 0) {
            score += 220;
        }

        return score;
    }

    private int minimax(GameModel model, int depth, int aiPlayerId, AiDifficulty level, int alpha, int beta) {
        if (model.getState().isGameOver()) {
            return terminalScore(model, aiPlayerId);
        }

        if (depth <= 0) {
            return evaluate(model, aiPlayerId);
        }

        int currentPlayerId = model.getState().getCurrentPlayerId();
        List<Action> actions = getCandidateActions(model, currentPlayerId, level);
        if (actions.isEmpty()) {
            int score = evaluate(model, aiPlayerId);
            return score;
        }

        int bestScore;

        if (currentPlayerId == aiPlayerId) {
            bestScore = -INF;

            for (Action action : actions) {
                GameModel nextModel = model.copy();
                if (!nextModel.play(action)) continue;

                int score = minimax(nextModel, depth - 1, aiPlayerId, level, alpha, beta);
                bestScore = Math.max(bestScore, score);


            }
        } else {
            bestScore = INF;

            for (Action action : actions) {
                GameModel nextModel = model.copy();
                if (!nextModel.play(action)) continue;

                int score = minimax(nextModel, depth - 1, aiPlayerId, level, alpha, beta);
                bestScore = Math.min(bestScore, score);
                
            }
        }

        return bestScore;
    }

    private int terminalScore(GameModel model, int aiPlayerId) {
        Player winner = model.getState().getWinner();
        if (winner == null) return 0;
        return winner.getId() == aiPlayerId ? WIN_SCORE : -WIN_SCORE ;
    }

    private List<Action> getCandidateActions(GameModel model, int playerId, AiDifficulty level) {
        Player player = model.getState().getPlayerById(playerId);
        List<Action> actions = new ArrayList<>();
        actions.addAll(model.getPossibleMoveActions(player));
        actions.addAll(getRankedWallActions(model, player, level));
        return rankActionsForPlayer(model, actions, playerId, level);
    }

    private List<WallAction> getRankedWallActions(GameModel model, Player player, AiDifficulty level) {
        if (player.getWallsLeft() <= 0 || level.getWallCandidateLimit() <= 0) return Collections.emptyList();

        Player opponent = model.getState().getOpponent(player.getId());
        
        List<Position> opponentPath = pathFinder.shortestPath(model.getState(), opponent);
        if (opponentPath == null || opponentPath.size() < 2) return Collections.emptyList();

        Set<Wall> candidateWalls = new LinkedHashSet<>();

        int maxDepth = Math.min(opponentPath.size() - 1, getPathEdgeLimit(level));
        for (int i = 0; i < maxDepth; i++) {
            Position current = opponentPath.get(i);
            Position next = opponentPath.get(i + 1);
            addBlockingWallCandidates(candidateWalls, current, next);
        }

        for (MoveAction opponentMove : model.getPossibleMoveActions(opponent)) {
            Position target = opponentMove.getTarget();
            if (target.getRow() == opponent.getGoalRow() || level != AiDifficulty.BEGINNER) {
                addBlockingWallCandidates(candidateWalls, opponent.getPosition(), target);
            }
        }

        int myCurrentPath = normalizedPathLength(model.shortestPathLength(player));
        int oppCurrentPath = opponentPath.size() - 1;

        List<ScoredWallAction> scoredWalls = new ArrayList<>();
        for (Wall wall : candidateWalls) {
            WallAction action = new WallAction(player.getId(), wall);
            GameModel nextModel = model.copy();
            if (!nextModel.play(action)) continue;

            int myNewPath = normalizedPathLength(nextModel.shortestPathLength(nextModel.getState().getPlayerById(player.getId())));
            int oppNewPath = normalizedPathLength(nextModel.shortestPathLength(nextModel.getState().getOpponent(player.getId())));

            int opponentDelay = oppNewPath - oppCurrentPath;
            int selfDelay = myNewPath - myCurrentPath;
            if (opponentDelay > 0 && isWallStrategicallySafe(level,
                    myCurrentPath, oppCurrentPath, selfDelay, opponentDelay)) {
                scoredWalls.add(new ScoredWallAction(action, scoreWallCandidate(
                    level, myCurrentPath, oppCurrentPath, selfDelay, opponentDelay, myNewPath, oppNewPath)));
            }
        }

        scoredWalls.sort(Comparator.comparingInt(ScoredWallAction::getScore).reversed());

        List<WallAction> actions = new ArrayList<>();
        int limit = Math.min(level.getWallCandidateLimit(), scoredWalls.size());
        for (int index = 0; index < limit; index++) {
            actions.add(scoredWalls.get(index).getAction());
        }
        return actions;
    }

    private List<Action> rankActionsForPlayer(GameModel model, List<Action> actions, int playerId,
            AiDifficulty level) {
        List<ScoredAction> scoredActions = new ArrayList<>();
        for (Action action : actions) {
            GameModel nextModel = model.copy();
            if (!nextModel.play(action)) continue;
            scoredActions.add(new ScoredAction(action,
                evaluate(nextModel, playerId) + tempoScore(model, nextModel, playerId, level)));
        }
        scoredActions.sort(Comparator.comparingInt(ScoredAction::getScore).reversed());

        List<Action> rankedActions = new ArrayList<>();
        for (ScoredAction scoredAction : scoredActions) {
            rankedActions.add(scoredAction.getAction());
        }
        return rankedActions;
    }

    private int getPathEdgeLimit(AiDifficulty level) {
        if (level == AiDifficulty.BEGINNER) return 2;
        if (level == AiDifficulty.INTERMEDIATE) return 5;
        return 7;
    }

    private boolean isWallStrategicallySafe(AiDifficulty level, int myCurrentPath, int oppCurrentPath,
            int selfDelay, int opponentDelay) {
        if (selfDelay <= maxSelfDelayForWall(level, myCurrentPath, oppCurrentPath)) {
            return true;
        }

        return oppCurrentPath <= 2 && opponentDelay >= selfDelay;
    }

    private int maxSelfDelayForWall(AiDifficulty level, int myCurrentPath, int oppCurrentPath) {
        if (myCurrentPath <= 2) {
            return oppCurrentPath <= 1 ? 1 : 0;
        }

        int limit = 0;
        if (level == AiDifficulty.INTERMEDIATE) {
            limit = 1;
        } else if (level == AiDifficulty.ADVANCED) {
            limit = 2;
        }

        if (oppCurrentPath <= 3) {
            limit++;
        }
        return limit;
    }

    private int scoreWallCandidate(AiDifficulty level, int myCurrentPath, int oppCurrentPath,
            int selfDelay, int opponentDelay, int myNewPath, int oppNewPath) {
        int urgency = Math.max(0, myCurrentPath - oppCurrentPath);
        int score = opponentDelay * (oppCurrentPath <= 3 ? 180 : 120);
        score -= Math.max(0, selfDelay) * (myCurrentPath <= 3 ? 150 : 85);
        score += urgency * 25;

        if (oppCurrentPath <= 2) {
            score += 240;
        }
        if (myCurrentPath <= 2 && selfDelay > 0) {
            score -= 260;
        }
        if (oppNewPath > myNewPath) {
            score += 50;
        }
        if (level == AiDifficulty.ADVANCED && opponentDelay >= 2) {
            score += 60;
        }
        return score;
    }

    private void addBlockingWallCandidates(Set<Wall> candidateWalls, Position from, Position to) {
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());

        if (rowDiff + colDiff == 1) {
            addBlockingWallCandidatesForEdge(candidateWalls, from, to);
            return;
        }

        if ((rowDiff == 2 && colDiff == 0) || (rowDiff == 0 && colDiff == 2)) {
            Position middle = new Position(
                (from.getRow() + to.getRow()) / 2,
                (from.getCol() + to.getCol()) / 2
            );
            addBlockingWallCandidatesForEdge(candidateWalls, from, middle);
            addBlockingWallCandidatesForEdge(candidateWalls, middle, to);
        }
    }

    private void addBlockingWallCandidatesForEdge(Set<Wall> candidateWalls, Position from, Position to) {
        if (from.getCol() == to.getCol()) {
            int row = Math.min(from.getRow(), to.getRow());
            addWallCandidate(candidateWalls, row, from.getCol(), WallOrientation.HORIZONTAL);
            addWallCandidate(candidateWalls, row, from.getCol() - 1, WallOrientation.HORIZONTAL);
        } else if (from.getRow() == to.getRow()) {
            int col = Math.min(from.getCol(), to.getCol());
            addWallCandidate(candidateWalls, from.getRow(), col, WallOrientation.VERTICAL);
            addWallCandidate(candidateWalls, from.getRow() - 1, col, WallOrientation.VERTICAL);
        }
    }

    
    private void addWallCandidate(Set<Wall> candidateWalls, int row, int col, WallOrientation orientation) {
        if (row < 0 || row >= Board.WALL_GRID_SIZE || col < 0 || col >= Board.WALL_GRID_SIZE) return;
        candidateWalls.add(new Wall(row, col, orientation));
    }

    private int evaluate(GameModel model, int playerId) {
        GameState state = model.getState();
        Player player = state.getPlayerById(playerId);
        Player opponent = state.getOpponent(playerId);

        if (player.hasWon()) return WIN_SCORE;
        if (opponent.hasWon()) return -WIN_SCORE;

        int playerPath = normalizedPathLength(model.shortestPathLength(player));
        int opponentPath = normalizedPathLength(model.shortestPathLength(opponent));
        
        int score = (opponentPath - playerPath) * 100;

        int wallValue = 20 + Math.max(0, 10 - playerPath) * 10;
        score += (player.getWallsLeft() - opponent.getWallsLeft()) * wallValue;

        if (playerPath <= 1) {
            score += 800;
        }
        if (opponentPath <= 1) {
            score -= 850;
        }
        
        return score;
    }

    private int normalizedPathLength(int pathLength) {
        return pathLength < 0 ? 99 : pathLength;
    }

    private static class ScoredAction {
        private final Action action;
        private final int score;

        ScoredAction(Action action, int score) {
            this.action = action;
            this.score = score;
        }

        Action getAction() { return action; }
        int getScore() { return score; }
    }

    private static class ScoredWallAction {
        private final WallAction action;
        private final int score;

        ScoredWallAction(WallAction action, int score) {
            this.action = action;
            this.score = score;
        }

        WallAction getAction() { return action; }
        int getScore() { return score; }
    }
}