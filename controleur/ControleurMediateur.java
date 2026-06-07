package controleur;

import modele.GameModel;
import vue.GameView;
import vue.InterfaceGraphique;
import ia.AiDifficulty;

import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;

public class ControleurMediateur {
    private static final int HINTS_PER_PLAYER = 2;

    private final InterfaceGraphique interfaceGraphique;
    private final SavedGameStore saveStore;
    private String selectedGameMode;
    private AiDifficulty selectedPlayer1AiLevel;
    private AiDifficulty selectedPlayer2AiLevel;
    private GameController currentController;
    private final AudioManager audioManager;
    private boolean soundEnabled;
    private boolean musicEnabled;

    public ControleurMediateur() {
        this.interfaceGraphique = new InterfaceGraphique();
        this.saveStore = new SavedGameStore();
        this.selectedGameMode = "Player vs Player";
        this.selectedPlayer1AiLevel = null;
        this.selectedPlayer2AiLevel = null;
        this.currentController = null;
        this.audioManager = new AudioManager();
        this.soundEnabled = false;
        this.musicEnabled = false;
        installActions();
    }

    public void demarrer() {
        interfaceGraphique.afficher();
    }

    private void installActions() {
        interfaceGraphique.onNouvellePartie(event -> interfaceGraphique.afficherMode());
        interfaceGraphique.onQuitter(event -> fermer());
        interfaceGraphique.onModePlayerVsPlayer(event -> startNewGame("Player vs Player", null, null));
        interfaceGraphique.onModePlayerVsAi(event -> chooseDifficultyFor("Player vs AI"));
        interfaceGraphique.onModeAiVsAi(event -> chooseAiVsAiDifficulties());
        interfaceGraphique.onReprendrePartie(event -> reprendrePartie());
        interfaceGraphique.onRetourMode(event -> interfaceGraphique.afficherMenu());
        interfaceGraphique.onNiveauBeginner(event -> startNewGameWithSelectedLevel(AiDifficulty.BEGINNER));
        interfaceGraphique.onNiveauAmateur(event -> startNewGameWithSelectedLevel(AiDifficulty.INTERMEDIATE));
        interfaceGraphique.onNiveauProfessional(event -> startNewGameWithSelectedLevel(AiDifficulty.ADVANCED));
        interfaceGraphique.onRetourDifficulte(event -> interfaceGraphique.afficherMode());
        interfaceGraphique.onMusic(event -> toggleMusic());
    }

    private void chooseDifficultyFor(String gameMode) {
        selectedGameMode = gameMode;
        interfaceGraphique.afficherDifficulte();
    }

    private void chooseAiVsAiDifficulties() {
        selectedGameMode = "AI vs AI";
        AiDifficulty[] levels = interfaceGraphique.choisirDifficultesAiVsAi(
            defaultAiLevel(selectedPlayer1AiLevel),
            defaultAiLevel(selectedPlayer2AiLevel)
        );
        if (levels == null) {
            return;
        }

        startNewGame("AI vs AI", levels[0], levels[1]);
    }

    private void startNewGameWithSelectedLevel(AiDifficulty aiLevel) {
        if ("AI vs AI".equals(selectedGameMode)) {
            startNewGame(selectedGameMode, aiLevel, aiLevel);
        } else {
            startNewGame(selectedGameMode, null, aiLevel);
        }
    }

    private void startNewGame(String gameMode, AiDifficulty player1AiLevel, AiDifficulty player2AiLevel) {
        if (currentController != null) {
            currentController.dispose();
        }

        selectedGameMode = gameMode;
        selectedPlayer1AiLevel = player1AiLevel;
        selectedPlayer2AiLevel = player2AiLevel;

        startGame(new GameModel(), selectedGameMode, selectedPlayer1AiLevel, selectedPlayer2AiLevel,
            HINTS_PER_PLAYER, HINTS_PER_PLAYER);
    }

    private void startGame(GameModel model, String gameMode,
            AiDifficulty player1AiLevel, AiDifficulty player2AiLevel,
            int player1HintsLeft, int player2HintsLeft) {
        GameView gameView = new GameView(model);
        currentController = new GameController(model, gameView, gameMode, player1AiLevel, player2AiLevel,
            player1HintsLeft, player2HintsLeft, audioManager);
        currentController.setAiDifficultyChangeListener((player1Level, player2Level) -> {
            selectedPlayer1AiLevel = player1Level;
            selectedPlayer2AiLevel = player2Level;
        });
        gameView.addMenuListener(event -> afficherMenu());
        gameView.addRestartListener(event -> restartCurrentGame());
        audioManager.setEffectsEnabled(soundEnabled);
        gameView.setSoundEnabled(soundEnabled);
        gameView.addSoundListener(event -> {
            toggleSound(gameView);
            gameView.requestBoardFocusLater();
        });
        gameView.setMusicEnabled(musicEnabled);
        gameView.addMusicListener(event -> {
            toggleMusic();
            gameView.requestBoardFocusLater();
        });

        interfaceGraphique.afficherJeu(gameView);
    }

    private void toggleSound(GameView gameView) {
        soundEnabled = !soundEnabled;
        audioManager.setEffectsEnabled(soundEnabled);
        gameView.setSoundEnabled(soundEnabled);

        if (!soundEnabled) {
            gameView.setStatusText("Effets sonores coupes.");
            return;
        }

        if (!audioManager.isWallEffectAvailable() && !audioManager.isMoveEffectAvailable()) {
            gameView.setStatusText("Effets actives, mais les sons sont indisponibles.");
            return;
        }

        gameView.setStatusText("Effets sonores actives.");
    }

    private void toggleMusic() {
        boolean enabling = !musicEnabled;
        boolean playbackSucceeded = audioManager.setMusicEnabled(enabling);
        musicEnabled = enabling && playbackSucceeded;
        interfaceGraphique.setMusicEnabled(musicEnabled);

        if (currentController != null) {
            if (!enabling) {
                currentController.updateExternalStatus("Musique coupee.");
            } else if (!playbackSucceeded) {
                currentController.updateExternalStatus("Lecture musique impossible : " + audioManager.getLastError() + ".");
            } else {
                currentController.updateExternalStatus("Musique activee.");
            }
        }
    }

    private void reprendrePartie() {
        List<SavedGameStore.SavedGameInfo> saves;
        try {
            saves = saveStore.listSaves();
        } catch (IOException exception) {
            interfaceGraphique.afficherMessage(
                "Reprendre une partie",
                "Impossible de lire les sauvegardes.",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (saves.isEmpty()) {
            interfaceGraphique.afficherMessage(
                "Reprendre une partie",
                "Aucune sauvegarde disponible.",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        while (true) {
            Object selected = interfaceGraphique.choisirDansListe(
                "Reprendre une partie",
                "Choisis une sauvegarde :",
                saves.toArray(new SavedGameStore.SavedGameInfo[0])
            );
            if (!(selected instanceof SavedGameStore.SavedGameInfo)) {
                return;
            }

            SavedGameStore.SavedGameInfo selectedSave = (SavedGameStore.SavedGameInfo) selected;
            int action = interfaceGraphique.choisirActionSauvegarde(selectedSave);
            if (action == InterfaceGraphique.SAVE_ACTION_LOAD) {
                loadSavedGame(selectedSave);
                return;
            }
            if (action == InterfaceGraphique.SAVE_ACTION_DELETE) {
                if (deleteSavedGame(selectedSave)) {
                    try {
                        saves = saveStore.listSaves();
                    } catch (IOException exception) {
                        interfaceGraphique.afficherMessage(
                            "Reprendre une partie",
                            "Impossible de lire les sauvegardes.",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    if (saves.isEmpty()) {
                        interfaceGraphique.afficherMessage(
                            "Reprendre une partie",
                            "Aucune sauvegarde disponible.",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                    }
                }
            } else {
                return;
            }
        }
    }

    private void loadSavedGame(SavedGameStore.SavedGameInfo selectedSave) {
        try {
            SavedGameStore.SavedGame savedGame = saveStore.load(selectedSave);
            startLoadedGame(savedGame);
        } catch (IOException | ClassNotFoundException exception) {
            interfaceGraphique.afficherMessage(
                "Reprendre une partie",
                "Impossible d'ouvrir cette sauvegarde.",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private boolean deleteSavedGame(SavedGameStore.SavedGameInfo selectedSave) {
        if (!interfaceGraphique.confirmerSuppressionSauvegarde(selectedSave)) {
            return false;
        }

        try {
            saveStore.delete(selectedSave);
            interfaceGraphique.afficherMessage(
                "Supprimer une sauvegarde",
                "Sauvegarde supprimee.",
                JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        } catch (IOException exception) {
            interfaceGraphique.afficherMessage(
                "Supprimer une sauvegarde",
                "Impossible de supprimer cette sauvegarde.",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    private void startLoadedGame(SavedGameStore.SavedGame savedGame) {
        if (currentController != null) {
            currentController.dispose();
        }

        selectedGameMode = savedGame.getGameMode() == null ? "Player vs Player" : savedGame.getGameMode();
        selectedPlayer1AiLevel = savedGame.getPlayer1AiDifficulty();
        selectedPlayer2AiLevel = savedGame.getPlayer2AiDifficulty();

        startGame(savedGame.getModel(), selectedGameMode, selectedPlayer1AiLevel, selectedPlayer2AiLevel,
            savedGame.getPlayer1HintsLeft(), savedGame.getPlayer2HintsLeft());
    }

    private void restartCurrentGame() {
        startNewGame(selectedGameMode, selectedPlayer1AiLevel, selectedPlayer2AiLevel);
    }

    private AiDifficulty defaultAiLevel(AiDifficulty aiLevel) {
        return aiLevel == null ? AiDifficulty.INTERMEDIATE : aiLevel;
    }

    private void afficherMenu() {
        if (currentController != null) {
            currentController.dispose();
        }

        interfaceGraphique.afficherMenu();
    }

    private void fermer() {
        if (currentController != null) {
            currentController.dispose();
        }

        audioManager.close();
        interfaceGraphique.fermer();
        System.exit(0);
    }
}
