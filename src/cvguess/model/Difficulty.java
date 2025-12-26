package cvguess.model;

public enum Difficulty {
    EASY("Easy (Pixelation)"),
    MEDIUM("Medium (Gaussian Blur)"),
    HARD("Hard (Thresholding)"),
    EXTREME("Extreme (Edge Detection)");

    public final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
