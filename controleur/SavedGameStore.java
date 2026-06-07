package controleur;

import ia.AiDifficulty;
import modele.GameModel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SavedGameStore {
    private static final String SAVE_EXTENSION = ".qsave";
    private static final int DEFAULT_HINTS_PER_PLAYER = 2;
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Path saveDirectory;

    public SavedGameStore() {
        this(Paths.get(System.getProperty("user.home"), ".quoridor", "saves"));
    }

    SavedGameStore(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    public void save(String name, GameModel model, String gameMode, AiDifficulty aiDifficulty)
            throws IOException {
        save(name, model, gameMode, aiDifficulty, DEFAULT_HINTS_PER_PLAYER, DEFAULT_HINTS_PER_PLAYER);
    }

    public void save(String name, GameModel model, String gameMode, AiDifficulty aiDifficulty,
            int player1HintsLeft, int player2HintsLeft) throws IOException {
        save(name, model, gameMode, aiDifficulty, aiDifficulty, player1HintsLeft, player2HintsLeft);
    }

    public void save(String name, GameModel model, String gameMode,
            AiDifficulty player1AiDifficulty, AiDifficulty player2AiDifficulty,
            int player1HintsLeft, int player2HintsLeft) throws IOException {
        String trimmedName = cleanName(name);
        Files.createDirectories(saveDirectory);

        SavedGame savedGame = new SavedGame(trimmedName, model, gameMode,
            player1AiDifficulty, player2AiDifficulty,
            player1HintsLeft, player2HintsLeft, System.currentTimeMillis());
        Path savePath = pathForName(trimmedName);

        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(savePath))) {
            output.writeObject(savedGame);
        }
    }

    public SavedGame load(SavedGameInfo info) throws IOException, ClassNotFoundException {
        if (info == null) {
            throw new IllegalArgumentException("Aucune sauvegarde selectionnee.");
        }

        return read(info.file);
    }

    public void delete(SavedGameInfo info) throws IOException {
        if (info == null) {
            throw new IllegalArgumentException("Aucune sauvegarde selectionnee.");
        }

        Files.deleteIfExists(info.file);
    }

    public List<SavedGameInfo> listSaves() throws IOException {
        List<SavedGameInfo> saves = new ArrayList<>();
        if (!Files.isDirectory(saveDirectory)) {
            return saves;
        }

        try (java.util.stream.Stream<Path> files = Files.list(saveDirectory)) {
            files
                .filter(path -> path.getFileName().toString().endsWith(SAVE_EXTENSION))
                .forEach(path -> addSaveInfoIfReadable(saves, path));
        }

        saves.sort(Comparator.comparingLong(SavedGameInfo::getSavedAtMillis).reversed()
            .thenComparing(SavedGameInfo::getName));
        return saves;
    }

    private void addSaveInfoIfReadable(List<SavedGameInfo> saves, Path path) {
        try {
            SavedGame savedGame = read(path);
            saves.add(new SavedGameInfo(path, savedGame.getName(), savedGame.getSavedAtMillis()));
        } catch (IOException | ClassNotFoundException exception) {
            // Une sauvegarde illisible ne doit pas bloquer les autres.
        }
    }

    private SavedGame read(Path path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
            Object object = input.readObject();
            if (!(object instanceof SavedGame)) {
                throw new IOException("Format de sauvegarde invalide.");
            }

            return (SavedGame) object;
        }
    }

    private Path pathForName(String name) {
        return saveDirectory.resolve(slugForName(name) + SAVE_EXTENSION);
    }

    private String cleanName(String name) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Le nom de la sauvegarde est vide.");
        }

        return trimmedName;
    }

    private String slugForName(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT);
        String slug = normalized
            .replaceAll("[^a-z0-9._-]+", "_")
            .replaceAll("^_+|_+$", "");

        return slug.isEmpty() ? "partie" : slug;
    }

    public static final class SavedGame implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final GameModel model;
        private final String gameMode;
        private final AiDifficulty aiDifficulty;
        private final AiDifficulty player1AiDifficulty;
        private final AiDifficulty player2AiDifficulty;
        private final boolean hintsSaved;
        private final int player1HintsLeft;
        private final int player2HintsLeft;
        private final long savedAtMillis;

        private SavedGame(String name, GameModel model, String gameMode,
                AiDifficulty player1AiDifficulty, AiDifficulty player2AiDifficulty,
                int player1HintsLeft, int player2HintsLeft, long savedAtMillis) {
            this.name = name;
            this.model = model;
            this.gameMode = gameMode;
            this.aiDifficulty = player2AiDifficulty == null ? player1AiDifficulty : player2AiDifficulty;
            this.player1AiDifficulty = player1AiDifficulty;
            this.player2AiDifficulty = player2AiDifficulty;
            this.hintsSaved = true;
            this.player1HintsLeft = clampHints(player1HintsLeft);
            this.player2HintsLeft = clampHints(player2HintsLeft);
            this.savedAtMillis = savedAtMillis;
        }

        public String getName() {
            return name;
        }

        public GameModel getModel() {
            return model;
        }

        public String getGameMode() {
            return gameMode;
        }

        public AiDifficulty getAiDifficulty() {
            return aiDifficulty;
        }

        public AiDifficulty getPlayer1AiDifficulty() {
            return player1AiDifficulty == null ? aiDifficulty : player1AiDifficulty;
        }

        public AiDifficulty getPlayer2AiDifficulty() {
            return player2AiDifficulty == null ? aiDifficulty : player2AiDifficulty;
        }

        public int getPlayer1HintsLeft() {
            return hintsSaved ? clampHints(player1HintsLeft) : DEFAULT_HINTS_PER_PLAYER;
        }

        public int getPlayer2HintsLeft() {
            return hintsSaved ? clampHints(player2HintsLeft) : DEFAULT_HINTS_PER_PLAYER;
        }

        public long getSavedAtMillis() {
            return savedAtMillis;
        }

        private static int clampHints(int hintsLeft) {
            return Math.max(0, Math.min(DEFAULT_HINTS_PER_PLAYER, hintsLeft));
        }
    }

    public static final class SavedGameInfo {
        private final Path file;
        private final String name;
        private final long savedAtMillis;

        private SavedGameInfo(Path file, String name, long savedAtMillis) {
            this.file = file;
            this.name = name;
            this.savedAtMillis = savedAtMillis;
        }

        public String getName() {
            return name;
        }

        public long getSavedAtMillis() {
            return savedAtMillis;
        }

        @Override
        public String toString() {
            if (savedAtMillis <= 0L) {
                return name;
            }

            LocalDateTime savedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(savedAtMillis),
                ZoneId.systemDefault()
            );
            return name + " - " + DISPLAY_DATE_FORMAT.format(savedAt);
        }
    }
}
