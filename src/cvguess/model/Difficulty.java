package cvguess.model;

public enum Difficulty {
    EASY("Easy (Edge Detection)"),
    MEDIUM("Medium (Thresholding)"),
    HARD("Hard (Gaussian Blur)"),
    EXTREME("Extreme (Pixelation)");

    public final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
