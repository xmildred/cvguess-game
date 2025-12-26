package cvguess.core;

import cvguess.model.Category;
import cvguess.model.Leaderboard;
import cvguess.model.Player;
import cvguess.model.Difficulty;

import java.awt.image.BufferedImage;

public class GameController {

    public static class RoundState {
        public final BufferedImage shownImage;
        public final int timeLeft;
        public final int correctCount;
        public final int wrongTries;
        public final String debugFileName; // istersen görünmesin diye UI’da kullanma

        public RoundState(BufferedImage shownImage, int timeLeft, int correctCount, int wrongTries,
                String debugFileName) {
            this.shownImage = shownImage;
            this.timeLeft = timeLeft;
            this.correctCount = correctCount;
            this.wrongTries = wrongTries;
            this.debugFileName = debugFileName;
        }
    }

    private final ImageRepository repo;
    private final ImageProcessor processor = new ImageProcessor();
    private final PixelBlockGenerator generator = new PixelBlockGenerator();
    private final Leaderboard leaderboard;

    private Player player;
    private Category category;

    private int timeLeft = 30;
    private int wrongTries = 0;

    private boolean gameWon = false;
    private final java.util.List<String> imageQueue = new java.util.ArrayList<>();

    private ImageRepository.ImageItem current;

    // Parametreler (zorluk)
    private static final int TARGET_SIZE = 360;

    private Difficulty difficulty = Difficulty.EASY;

    public GameController(ImageRepository repo, Leaderboard leaderboard) {
        this.repo = repo;
        this.leaderboard = leaderboard;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    // ... (rest of methods)

    public void startNewGame(String playerName, Category category) {
        this.player = new Player(playerName);
        this.category = category;
        this.timeLeft = 60; // 30 -> 60 saniye
        this.wrongTries = 0;
        this.gameWon = false;

        // Initialize and shuffle queue
        imageQueue.clear();
        imageQueue.addAll(repo.listImageNames(category));
        java.util.Collections.shuffle(imageQueue);

        nextImage();

        if (current == null && !gameWon) {
            throw new RuntimeException("Görsel yüklenemedi! Lütfen 'images' klasörünü kontrol edin.");
        }
    }

    public RoundState getCurrentState() {
        BufferedImage shown = buildShownImage();
        return new RoundState(shown, timeLeft, player.getCorrectCount(), wrongTries,
                current != null ? current.rawName : "");
    }

    public boolean isGameOver() {
        return timeLeft <= 0 || gameWon;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public void tickOneSecond() {
        timeLeft--;
    }

    public enum GuessResult {
        CORRECT, WRONG, GAME_OVER, GAME_WON
    }

    public GuessResult submitGuess(String guessRaw) {
        if (gameWon)
            return GuessResult.GAME_WON;
        if (current == null)
            return GuessResult.GAME_OVER;
        if (timeLeft <= 0)
            return GuessResult.GAME_OVER;

        String g = ImageRepository.normalizeAnswer(guessRaw);
        if (g.isEmpty())
            return GuessResult.WRONG;

        boolean ok = isMatch(g, current.answer);
        if (ok) {
            player.incrementCorrect();
            timeLeft += 3;
            wrongTries = 0;
            nextImage();

            if (gameWon)
                return GuessResult.GAME_WON;

            return GuessResult.CORRECT;
        } else {
            timeLeft -= 2;
            wrongTries++;
            if (timeLeft <= 0)
                return GuessResult.GAME_OVER;
            return GuessResult.WRONG;
        }
    }

    private boolean isMatch(String guess, String answer) {
        // Basit eşleşme: tam eşit veya “answer” kelimesini içeriyor
        if (guess.equals(answer))
            return true;
        if (guess.contains(answer))
            return true;
        if (answer.contains(guess) && guess.length() >= 3)
            return true;
        return false;
    }

    private void nextImage() {
        if (imageQueue.isEmpty()) {
            gameWon = true;
            current = null;
            return;
        }

        String nextName = imageQueue.remove(0);
        current = repo.loadImage(category, nextName);

        if (current == null) {
            // If load fails, try next
            System.err.println("Failed to load image: " + nextName + ", picking next...");
            nextImage();
        }
    }

    private BufferedImage buildShownImage() {
        if (current == null) {
            System.out.println("Current image is null! Returning placeholder.");
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        }

        BufferedImage base = processor.centerCropAndResize(current.image, TARGET_SIZE);

        // Base progression factor: 0.0 (start) -> 1.0 (end) approximately
        // wrongTries 0 -> 0.0
        // wrongTries 5 -> 0.5
        // wrongTries 10 -> 1.0
        // But game logic is discrete. Let's adapt based on difficulty.

        switch (difficulty) {
            case EASY:
                return generateEasyMode(base);
            case MEDIUM:
                return generateMediumMode(base);
            case HARD:
                return generateHardMode(base);
            case EXTREME:
                return generateExtremeMode(base);
            default:
                return generateEasyMode(base);
        }
    }

    // Easy: Pixelation (Existing logic)
    private BufferedImage generateEasyMode(BufferedImage base) {
        boolean twoBlocks = (wrongTries == 0);
        int gridN = Math.min(20, 2 + wrongTries * 2);
        int pixelFactor = Math.max(1, 12 - wrongTries * 4);
        int colorLevels = Math.min(64, 8 + wrongTries * 8);

        BufferedImage quant = processor.quantizeColors(base, colorLevels);
        return generator.generate(quant, gridN, twoBlocks, pixelFactor);
    }

    // Medium: Gaussian Blur
    // Start: High Radius (e.g. 40) -> End: 0 (Sharp)
    private BufferedImage generateMediumMode(BufferedImage base) {
        int maxRadius = 40;
        int decreaseStep = 5;

        int radius = Math.max(0, maxRadius - (wrongTries * decreaseStep));
        if (radius == 0)
            return base;

        return processor.applyBlur(base, radius);
    }

    // Hard: Thresholding
    // Start: High Contrast / Silhouette -> End: More gray/color
    // Actually strategy: Start with binary threshold, then slowly mix with real
    // image?
    // Or just lower/raise the threshold to reveal more?
    // Let's try: "Start as binary black/white, then add grays, then color"
    // Stage 1 (0-2 wrong): Binary Threshold (Silhouette)
    // Stage 2 (3-5 wrong): Grayscale (Quantized)
    // Stage 3 (6+ wrong): Color (Quantized -> Full)
    private BufferedImage generateHardMode(BufferedImage base) {
        if (wrongTries < 3) {
            // Binary Threshold - changing threshold level might be hint?
            // Maybe fixed threshold: 128
            return processor.applyThreshold(base, 128);
        } else if (wrongTries < 6) {
            // Grayscale (using 0 saturation trick or just standard gray conversion if we
            // had it)
            // Let's use quantizeColors with very low levels on grayscale input?
            // For now, let's just reuse applyThreshold but with varied thresholds to show
            // it's 'hard'
            // Actually, let's implement a simple "Grayscale" fallback by just returning
            // a heavily quantized color version, which matches "Hard" progression.
            // OR: simulate "revealing" by mixing.
            // Let's stick to user request: "Thresholding".
            // Maybe we reveal by changing threshold?
            // T=200 (mostly black), T=150, T=100...
            // Let's go with: Start T=50, End T=200? No that changes what is visible.

            // Strategy: Mix Threshold result with Original.
            // But we don't have mix function.
            // Let's fallback to: Color Quantization 2 levels (Binary-ish) -> 4 levels -> 8
            // levels
            return processor.quantizeColors(base, 2 + (wrongTries - 3) * 2);
        } else {
            // Near full color
            return processor.quantizeColors(base, 8 + (wrongTries - 6) * 8);
        }
    }

    // Extreme: Edge Detection
    // Start: Only Edges -> End: Mix with real image
    // Since we can't easily Mix without a new method, let's toggle?
    // Or: "Extreme" means it STAYS as Edge Detection for a long time.
    // Let's say: Always Edge Detection, but maybe we quantize valid colors "under"
    // it?
    // Simpler: Just return Edge Detection until very late stage?
    // The user said: "extreme ise edge detection ile olsun".
    // Let's enable "revealing" by checking wrongTries.
    private BufferedImage generateExtremeMode(BufferedImage base) {
        if (wrongTries < 8) {
            return processor.applyEdgeDetection(base);
        } else {
            // Give up, show blurry real image
            return processor.applyBlur(base, 10 - (wrongTries - 8));
        }
    }

    public void finalizeAndSave() {
        if (player == null)
            return;
        player.setFinalTimeSeconds(Math.max(0, timeLeft));
        leaderboard.add(player.getName(), player.getCorrectCount(), player.getFinalTimeSeconds());
    }

    public Player getPlayer() {
        return player;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public String getCurrentAnswer() {
        return current != null ? current.answer : "???";
    }

    public BufferedImage getCurrentImage() {
        return current != null ? current.image : null;
    }
}
