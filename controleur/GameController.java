package controleur;

import modele.Action;
import modele.GameModel;
import modele.MoveAction;
import modele.Player;
import modele.Position;
import modele.Wall;
import modele.WallAction;
import ia.AiDifficulty;
import ia.MinimaxAi;
import vue.BoardView;
import vue.GameView;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

public class GameController {
    private static final String MODE_PLAYER_VS_PLAYER = "Player vs Player";
    private static final String MODE_PLAYER_VS_AI = "Player vs AI";
    private static final String MODE_AI_VS_AI = "AI vs AI";
    private static final String WALL_IMPOSSIBLE_MESSAGE = "Impossible de mettre le mur ici";
    private static final int HINTS_PER_PLAYER = 2;

    private final GameModel model;
    private final GameView gameView;
    private final BoardView boardView;
    private final AudioManager audioManager;
    private final String gameMode;
    private AiDifficulty player1AiDifficulty;
    private AiDifficulty player2AiDifficulty;
    private final MinimaxAi minimaxAi;
    private final SavedGameStore saveStore;
    private boolean moveHighlightsVisible;
    private boolean moveZoneActive;
    private boolean aiThinking;
    private boolean active;
    private int player1HintsLeft;
    private int player2HintsLeft;
    private Wall cachedPreviewCandidate;
    private boolean cachedPreviewCandidateValid;
    private Timer aiDelayTimer;
    private SwingWorker<Action, Void> aiWorker;
    private BiConsumer<AiDifficulty, AiDifficulty> aiDifficultyChangeListener;

    public GameController(GameModel model, GameView gameView) {
        this(model, gameView, MODE_PLAYER_VS_PLAYER, AiDifficulty.INTERMEDIATE);
    }

    public GameController(GameModel model, GameView gameView, String gameMode, AiDifficulty aiDifficulty) {
        this(model, gameView, gameMode, aiDifficulty, HINTS_PER_PLAYER, HINTS_PER_PLAYER);
    }

    public GameController(GameModel model, GameView gameView, String gameMode, AiDifficulty aiDifficulty,
            int player1HintsLeft, int player2HintsLeft) {
        this(model, gameView, gameMode, aiDifficulty, aiDifficulty, player1HintsLeft, player2HintsLeft);
    }

    public GameController(GameModel model, GameView gameView, String gameMode,
            AiDifficulty player1AiDifficulty, AiDifficulty player2AiDifficulty,
            int player1HintsLeft, int player2HintsLeft) {
        this(model, gameView, gameMode, player1AiDifficulty, player2AiDifficulty,
            player1HintsLeft, player2HintsLeft, new AudioManager());
    }

    GameController(GameModel model, GameView gameView, String gameMode,
            AiDifficulty player1AiDifficulty, AiDifficulty player2AiDifficulty,
            int player1HintsLeft, int player2HintsLeft, AudioManager audioManager) {
        this.model = model;
        this.gameView = gameView;
        this.boardView = gameView.getBoardView();
        this.audioManager = audioManager == null ? new AudioManager() : audioManager;
        this.gameMode = gameMode;
        this.player1AiDifficulty = normalizeAiDifficulty(player1AiDifficulty);
        this.player2AiDifficulty = normalizeAiDifficulty(player2AiDifficulty);
        this.minimaxAi = new MinimaxAi();
        this.saveStore = new SavedGameStore();
        this.moveHighlightsVisible = false;
        this.moveZoneActive = false;
        this.aiThinking = false;
        this.active = true;
        this.player1HintsLeft = clampHints(player1HintsLeft);
        this.player2HintsLeft = clampHints(player2HintsLeft);
        this.cachedPreviewCandidate = null;
        this.cachedPreviewCandidateValid = false;
        this.aiDifficultyChangeListener = null;

        gameView.setHintControlsVisible(!isAiPlayer(1), !isAiPlayer(2));
        gameView.setAiDifficultyControlsVisible(hasAiPlayers());
        updateAiDifficultyButtonText();
        installListeners();
        updateStatus("");
        SwingUtilities.invokeLater(() -> {
            refreshMoveHighlights();
            scheduleAiTurnIfNeeded();
        });
    }

    public void dispose() {
        active = false;

        if (aiDelayTimer != null) {
            aiDelayTimer.stop();
        }

        if (aiWorker != null && !aiWorker.isDone()) {
            aiWorker.cancel(true);
        }
    }

    public void setAiDifficultyChangeListener(BiConsumer<AiDifficulty, AiDifficulty> listener) {
        this.aiDifficultyChangeListener = listener;
    }

    public void updateExternalStatus(String message) {
        updateStatus(message);
    }

    private void installListeners() {
        boardView.setFocusable(true);
        boardView.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                boardView.requestFocusInWindow();

                if (!canHumanPlay()) {
                    updateStatus("");
                    return;
                }

                updatePointerFeedback(event.getX(), event.getY());
                if (SwingUtilities.isRightMouseButton(event)) {
                    playWallAt(event.getX(), event.getY());
                } else if (SwingUtilities.isLeftMouseButton(event)) {
                    handleLeftClick(event.getX(), event.getY());
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                setMoveZoneActive(false);
                clearWallPreview();
            }
        });

        boardView.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                if (!canHumanPlay()) {
                    setMoveZoneActive(false);
                    clearWallPreview();
                    return;
                }

                updatePointerFeedback(event.getX(), event.getY());
            }
        });

        boardView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKeyPressed(event);
            }
        });

        gameView.addUndoListener(event -> undoLastAction());
        gameView.addRedoListener(event -> redoLastAction());
        gameView.addSaveListener(event -> saveCurrentGame());
        gameView.addHintListener(event -> showHint());
        gameView.addAiDifficultyListener(event -> changeAiDifficulty());
    }

    private void handleKeyPressed(KeyEvent event) {
        if (!canHumanPlay()) {
            updateStatus("");
            return;
        }

        switch (event.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_Z:
            case KeyEvent.VK_W:
                playMoveBy(-1, 0);
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                playMoveBy(1, 0);
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_Q:
            case KeyEvent.VK_A:
                playMoveBy(0, -1);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                playMoveBy(0, 1);
                break;
            case KeyEvent.VK_M:
                refreshMoveHighlights();
                break;
            default:
                break;
        }
    }

    private void playMoveBy(int rowDelta, int colDelta) {
        Player player = model.getState().getCurrentPlayer();
        Position position = player.getPosition();
        Position target = new Position(position.getRow() + rowDelta, position.getCol() + colDelta);
        Position opponentPosition = model.getState().getOpponent(player.getId()).getPosition();

        if (target.equals(opponentPosition)) {
            Position jumpTarget = new Position(position.getRow() + rowDelta * 2, position.getCol() + colDelta * 2);
            if (model.isMoveValid(player, jumpTarget)) {
                target = jumpTarget;
            }
        }

        playMove(target);
    }

    private void handleLeftClick(int x, int y) {
        if (boardView.wallAtPoint(x, y) != null) {
            playWallAt(x, y);
            return;
        }

        Position clickedPosition = boardView.positionAtPoint(x, y);
        Player currentPlayer = model.getState().getCurrentPlayer();

        if (clickedPosition != null && model.isMoveValid(currentPlayer, clickedPosition)) {
            playMove(clickedPosition);
            return;
        }

        if (isCurrentPlayerPosition(clickedPosition)) {
            updateStatus("");
            refreshMoveHighlights();
            return;
        }

        playWallAt(x, y);
    }

    private void playMove(Position target) {
        Player player = model.getState().getCurrentPlayer();
        int playerId = player.getId();
        boolean soundPlayedBeforeModelUpdate = canPlayMoveSoundBeforeModelUpdate(player, target);
        boolean moveSoundPlayed = !soundPlayedBeforeModelUpdate || audioManager.playMoveEffect();
        boolean success = model.play(new MoveAction(playerId, target));

        if (success) {
            if (!soundPlayedBeforeModelUpdate) {
                moveSoundPlayed = audioManager.playMoveEffect();
            }
            resetWallPreviewCache();
            clearWallPreview();
            boardView.clearHint();
            refreshAfterAction();
            showAudioErrorIfNeeded(moveSoundPlayed);
        } else {
            updateStatus("");
        }
    }

    private boolean canPlayMoveSoundBeforeModelUpdate(Player player, Position target) {
        return audioManager.isEffectsEnabled() && model.isMoveValid(player, target);
    }

    private void playWallAt(int x, int y) {
        Wall wall = boardView.wallAtPoint(x, y);
        if (wall == null) {
            updateStatus(WALL_IMPOSSIBLE_MESSAGE);
            return;
        }

        int playerId = model.getState().getCurrentPlayerId();
        boolean soundPlayedBeforeModelUpdate = canPlayWallSoundBeforeModelUpdate(wall);
        boolean wallSoundPlayed = !soundPlayedBeforeModelUpdate || audioManager.playWallEffect();
        boolean success = model.play(new WallAction(playerId, wall));

        if (success) {
            if (!soundPlayedBeforeModelUpdate) {
                wallSoundPlayed = audioManager.playWallEffect();
            }
            resetWallPreviewCache();
            clearWallPreview();
            boardView.clearHint();
            refreshAfterAction();
            showAudioErrorIfNeeded(wallSoundPlayed);
        } else {
            updateStatus(WALL_IMPOSSIBLE_MESSAGE);
        }
    }

    private boolean canPlayWallSoundBeforeModelUpdate(Wall wall) {
        return audioManager.isEffectsEnabled()
            && wall.equals(cachedPreviewCandidate)
            && cachedPreviewCandidateValid;
    }

    private void refreshMoveHighlights() {
        if (shouldShowMoveHighlights()) {
            moveHighlightsVisible = true;
            boardView.setHighlightedPositions(getPossibleMovePositions());
        } else {
            hideMoveHighlights();
        }
    }

    private void updateMoveHighlights() {
        if (shouldShowMoveHighlights()) {
            showMoveHighlights();
        } else {
            hideMoveHighlights();
        }
    }

    private void showMoveHighlights() {
        if (moveHighlightsVisible) {
            return;
        }

        moveHighlightsVisible = true;
        boardView.setHighlightedPositions(getPossibleMovePositions());
    }

    private void hideMoveHighlights() {
        moveHighlightsVisible = false;
        boardView.clearHighlightedPositions();
    }

    private boolean shouldShowMoveHighlights() {
        return canHumanPlay() && moveZoneActive;
    }

    private List<Position> getPossibleMovePositions() {
        List<Position> positions = new ArrayList<>();

        for (MoveAction action : model.getPossibleMoveActions(model.getState().getCurrentPlayer())) {
            positions.add(action.getTarget());
        }

        return positions;
    }

    private void updatePointerFeedback(int x, int y) {
        Player player = model.getState().getCurrentPlayer();
        boolean nearPawn = boardView.isPointInPawnNeighborhood(x, y, player.getPosition());
        Position pointedPosition = boardView.positionAtPoint(x, y);
        boolean onPossibleMove = pointedPosition != null && model.isMoveValid(player, pointedPosition);
        setMoveZoneActive(nearPawn || onPossibleMove);
        updateWallPreview(x, y);
    }

    private void setMoveZoneActive(boolean active) {
        if (moveZoneActive == active) {
            return;
        }

        moveZoneActive = active;
        updateMoveHighlights();
    }

    private void updateWallPreview(int x, int y) {
        Wall wall = boardView.wallAtPoint(x, y);

        if (wall == null) {
            boardView.setPreviewWall(null);
            return;
        }

        if (!wall.equals(cachedPreviewCandidate)) {
            cachedPreviewCandidate = wall;
            cachedPreviewCandidateValid = model.isWallPlacementValid(model.getState().getCurrentPlayer(), wall);
        }

        if (cachedPreviewCandidateValid) {
            boardView.setPreviewWall(wall);
        } else {
            boardView.setPreviewWall(null);
        }
    }

    private void clearWallPreview() {
        boardView.setPreviewWall(null);
    }

    private void resetWallPreviewCache() {
        cachedPreviewCandidate = null;
        cachedPreviewCandidateValid = false;
    }

    private void undoLastAction() {
        if (!active || aiThinking) {
            updateStatus("");
            return;
        }

        if (aiDelayTimer != null) {
            aiDelayTimer.stop();
        }

        resetWallPreviewCache();
        setMoveZoneActive(false);
        clearWallPreview();
        boardView.clearHint();

        if (model.undoLastAction() && MODE_PLAYER_VS_AI.equals(gameMode) && isAiTurn() && model.canUndo()) {
            model.undoLastAction();
        }

        boardView.repaint();
        updateStatus("");
        refreshMoveHighlights();
        gameView.requestBoardFocusLater();
    }

    private void redoLastAction() {
        if (!active || aiThinking) {
            updateStatus("");
            return;
        }

        if (aiDelayTimer != null) {
            aiDelayTimer.stop();
        }

        resetWallPreviewCache();
        setMoveZoneActive(false);
        clearWallPreview();
        boardView.clearHint();

        if (model.redoLastAction() && MODE_PLAYER_VS_AI.equals(gameMode) && isAiTurn() && model.canRedo()) {
            model.redoLastAction();
        }

        boardView.repaint();
        updateStatus("");
        refreshMoveHighlights();
        scheduleAiTurnIfNeeded();
        gameView.requestBoardFocusLater();
    }

    private void saveCurrentGame() {
        if (!active || aiThinking || model.getState().isGameOver()) {
            updateStatus("");
            return;
        }

        if (aiDelayTimer != null) {
            aiDelayTimer.stop();
        }

        String saveName = JOptionPane.showInputDialog(
            gameView,
            "Nom de la sauvegarde :",
            "Sauvegarder la partie",
            JOptionPane.PLAIN_MESSAGE
        );

        if (saveName == null) {
            scheduleAiTurnIfNeeded();
            gameView.requestBoardFocusLater();
            return;
        }

        try {
            saveStore.save(saveName, model, gameMode, player1AiDifficulty, player2AiDifficulty,
                player1HintsLeft, player2HintsLeft);
            updateStatus("Partie sauvegardee : " + saveName.trim());
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(gameView, exception.getMessage(), "Sauvegarde", JOptionPane.WARNING_MESSAGE);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                gameView,
                "Impossible de sauvegarder la partie.",
                "Sauvegarde",
                JOptionPane.ERROR_MESSAGE
            );
        }

        scheduleAiTurnIfNeeded();
        gameView.requestBoardFocusLater();
    }

    private void showHint() {
        if (!canHumanPlay()) {
            updateStatus("");
            return;
        }

        int playerId = model.getState().getCurrentPlayerId();
        int hintsLeft = getHintsLeft(playerId);
        if (hintsLeft <= 0) {
            updateStatus("J" + playerId + " n'a plus d'indices.");
            return;
        }

        resetWallPreviewCache();
        clearWallPreview();
        hideMoveHighlights();
        boardView.clearHint();

        Action hintAction = chooseHintAction();
        if (hintAction == null) {
            updateStatus("Aucun indice trouve.");
            refreshMoveHighlights();
            return;
        }

        setHintsLeft(playerId, hintsLeft - 1);
        if (hintAction instanceof MoveAction) {
            boardView.showHintPosition(((MoveAction) hintAction).getTarget());
            updateStatus("Indice J" + playerId + " : case conseillee. Restants : " + getHintsLeft(playerId));
        } else if (hintAction instanceof WallAction) {
            boardView.showHintWall(((WallAction) hintAction).getWall());
            updateStatus("Indice J" + playerId + " : mur conseille. Restants : " + getHintsLeft(playerId));
        } else {
            updateStatus("Indice utilise. Restants : " + getHintsLeft(playerId));
        }

        gameView.requestBoardFocusLater();
    }

    private Action chooseHintAction() {
        GameModel snapshot = model.copy();
        return new MinimaxAi().chooseAction(snapshot, AiDifficulty.INTERMEDIATE);
    }

    private void changeAiDifficulty() {
        if (!hasAiPlayers()) {
            updateStatus("");
            return;
        }

        if (aiThinking || !active || model.getState().isGameOver()) {
            updateStatus("Le niveau IA pourra etre change au prochain tour.");
            return;
        }

        if (aiDelayTimer != null) {
            aiDelayTimer.stop();
        }

        AiDifficulty[] selectedDifficulties = gameView.chooseAiDifficulties(
            isAiPlayer(1),
            isAiPlayer(2),
            player1AiDifficulty,
            player2AiDifficulty
        );
        if (selectedDifficulties != null) {
            if (isAiPlayer(1)) {
                player1AiDifficulty = normalizeAiDifficulty(selectedDifficulties[0]);
            }
            if (isAiPlayer(2)) {
                player2AiDifficulty = normalizeAiDifficulty(selectedDifficulties[1]);
            }
            if (aiDifficultyChangeListener != null) {
                aiDifficultyChangeListener.accept(player1AiDifficulty, player2AiDifficulty);
            }
            updateAiDifficultyButtonText();
            updateStatus(aiDifficultyStatus());
        }

        scheduleAiTurnIfNeeded();
        gameView.requestBoardFocusLater();
    }

    private String aiDifficultyStatus() {
        if (MODE_AI_VS_AI.equals(gameMode)) {
            return "IA J1 : " + player1AiDifficulty.getLabel() + " | IA J2 : " + player2AiDifficulty.getLabel();
        }

        return "IA : " + player2AiDifficulty.getLabel();
    }

    private void updateAiDifficultyButtonText() {
        if (MODE_AI_VS_AI.equals(gameMode)) {
            gameView.setAiDifficultyText(
                "Niveau IA",
                "J1 : " + player1AiDifficulty.getLabel(),
                "J2 : " + player2AiDifficulty.getLabel()
            );
        } else if (MODE_PLAYER_VS_AI.equals(gameMode)) {
            gameView.setAiDifficultyText("Niveau IA", player2AiDifficulty.getLabel());
        } else {
            gameView.setAiDifficultyText("Niveau IA");
        }
    }

    private int getHintsLeft(int playerId) {
        return playerId == 1 ? player1HintsLeft : player2HintsLeft;
    }

    private void setHintsLeft(int playerId, int hintsLeft) {
        if (playerId == 1) {
            player1HintsLeft = clampHints(hintsLeft);
        } else {
            player2HintsLeft = clampHints(hintsLeft);
        }
    }

    private int clampHints(int hintsLeft) {
        return Math.max(0, Math.min(HINTS_PER_PLAYER, hintsLeft));
    }

    private boolean isCurrentPlayerPosition(Position position) {
        return position != null && position.equals(model.getState().getCurrentPlayer().getPosition());
    }

    private void refreshAfterAction() {
        setMoveZoneActive(false);

        if (model.getState().isGameOver()) {
            hideMoveHighlights();
            clearWallPreview();
            boardView.repaint();
            updateStatus("");
            return;
        }

        boardView.repaintDynamicLayer();
        updateStatus("");
        refreshMoveHighlights();
        scheduleAiTurnIfNeeded();
    }

    private void updateStatus(String message) {
        Player currentPlayer = model.getState().getCurrentPlayer();
        gameView.setStatusText(message);
        gameView.setUndoEnabled(model.canUndo() && active && !aiThinking);
        gameView.setRedoEnabled(model.canRedo() && active && !aiThinking);
        gameView.setSaveEnabled(active && !aiThinking && !model.getState().isGameOver());
        gameView.setHintEnabled(canHumanPlay() && getHintsLeft(currentPlayer.getId()) > 0, getHintsLeft(currentPlayer.getId()));
        gameView.setAiDifficultyEnabled(hasAiPlayers() && active && !aiThinking && !model.getState().isGameOver());
        gameView.updateWallCounters(
            model.getState().getPlayer1().getWallsLeft(),
            model.getState().getPlayer2().getWallsLeft(),
            currentPlayer.getId()
        );
        gameView.updateHintCounters(player1HintsLeft, player2HintsLeft, currentPlayer.getId());
    }

    private boolean canHumanPlay() {
        return active && !aiThinking && !model.getState().isGameOver() && !isAiTurn();
    }

    private boolean isAiTurn() {
        return isAiPlayer(model.getState().getCurrentPlayerId());
    }

    private boolean isAiPlayer(int playerId) {
        if (MODE_AI_VS_AI.equals(gameMode)) {
            return true;
        }

        return MODE_PLAYER_VS_AI.equals(gameMode) && playerId == 2;
    }

    private boolean hasAiPlayers() {
        return MODE_PLAYER_VS_AI.equals(gameMode) || MODE_AI_VS_AI.equals(gameMode);
    }

    private AiDifficulty getAiDifficultyForPlayer(int playerId) {
        return playerId == 1 ? player1AiDifficulty : player2AiDifficulty;
    }

    private static AiDifficulty normalizeAiDifficulty(AiDifficulty aiDifficulty) {
        return aiDifficulty == null ? AiDifficulty.INTERMEDIATE : aiDifficulty;
    }

    private void scheduleAiTurnIfNeeded() {
        if (!active || aiThinking || model.getState().isGameOver() || !isAiTurn()) {
            return;
        }

        setMoveZoneActive(false);
        clearWallPreview();
        updateStatus("");

        if (aiDelayTimer != null && aiDelayTimer.isRunning()) {
            aiDelayTimer.stop();
        }

        aiDelayTimer = new Timer(MODE_AI_VS_AI.equals(gameMode) ? 550 : 350, event -> startAiTurn());
        aiDelayTimer.setRepeats(false);
        aiDelayTimer.start();
    }

    private void startAiTurn() {
        if (!active || aiThinking || model.getState().isGameOver() || !isAiTurn()) {
            return;
        }

        aiThinking = true;
        GameModel snapshot = model.copy();
        updateStatus("");

        aiWorker = new SwingWorker<Action, Void>() {
            @Override
            protected Action doInBackground() {
                return minimaxAi.chooseAction(
                    snapshot,
                    getAiDifficultyForPlayer(snapshot.getState().getCurrentPlayerId())
                );
            }

            @Override
            protected void done() {
                finishAiTurn(this);
            }
        };
        aiWorker.execute();
    }

    private void finishAiTurn(SwingWorker<Action, Void> worker) {
        if (!active || worker.isCancelled()) {
            aiThinking = false;
            return;
        }

        Action action;
        try {
            action = worker.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            aiThinking = false;
            updateStatus("");
            return;
        } catch (CancellationException exception) {
            aiThinking = false;
            return;
        } catch (ExecutionException exception) {
            aiThinking = false;
            updateStatus("");
            return;
        }

        aiThinking = false;

        if (action == null) {
            updateStatus("");
            return;
        }

        if (model.getState().isGameOver()
                || action.getPlayerId() != model.getState().getCurrentPlayerId()
                || !isAiPlayer(action.getPlayerId())) {
            return;
        }

        boolean success = model.play(action);
        if (success) {
            boolean soundPlayed = true;
            if (action instanceof WallAction) {
                soundPlayed = audioManager.playWallEffect();
            } else if (action instanceof MoveAction) {
                soundPlayed = audioManager.playMoveEffect();
            }
            resetWallPreviewCache();
            boardView.clearHint();
            refreshAfterAction();
            showAudioErrorIfNeeded(soundPlayed);
        } else {
            updateStatus("");
        }
    }

    private void showAudioErrorIfNeeded(boolean soundPlayed) {
        if (audioManager.isEffectsEnabled() && !soundPlayed) {
            updateStatus("Lecture audio impossible : " + audioManager.getLastError() + ".");
        }
    }
}
