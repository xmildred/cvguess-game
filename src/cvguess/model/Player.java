package cvguess.model;

public class Player {
    private final String name;
    private int correctCount;
    private int finalTimeSeconds;

    public Player(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public int getCorrectCount() { return correctCount; }
    public int getFinalTimeSeconds() { return finalTimeSeconds; }

    public void incrementCorrect() { correctCount++; }
    public void setFinalTimeSeconds(int s) { finalTimeSeconds = s; }
}
