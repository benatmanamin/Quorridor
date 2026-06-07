package ia;

public enum AiDifficulty {
    BEGINNER("Debutant", 1, 5, 4),
    INTERMEDIATE("Intermediaire", 2, 9, 2),
    ADVANCED("Avance", 3, 9, 1);

    private final String label;
    private final int searchDepth;
    private final int wallCandidateLimit;
    private final int topChoiceCount;

    AiDifficulty(String label, int searchDepth, int wallCandidateLimit, int topChoiceCount) {
        this.label = label;
        this.searchDepth = searchDepth;
        this.wallCandidateLimit = wallCandidateLimit;
        this.topChoiceCount = topChoiceCount;
    }

    public String getLabel() {
        return label;
    }

    public int getSearchDepth() {
        return searchDepth;
    }

    public int getWallCandidateLimit() {
        return wallCandidateLimit;
    }

    public int getTopChoiceCount() {
        return topChoiceCount;
    }

    @Override
    public String toString() {
        return label;
    }
}
