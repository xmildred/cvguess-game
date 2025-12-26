package cvguess.model;

public enum Category {
    ANIMAL("animal", "🐾 Hayvan"),
    FILM("film", "🎬 Film"),
    GAME("game", "🎮 Oyun"),
    LOGO("logo", "🖼️ Logo");

    public final String folderName;
    public final String displayName;

    Category(String folderName, String displayName) {
        this.folderName = folderName;
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
